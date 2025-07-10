package dev.lone.blocksinjector;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.alessiodp.libby.classloader.URLClassLoaderHelper;
import com.alessiodp.libby.logging.LogLevel;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Spigot uses a mirror for maven central, so sometimes it cannot find my library if I just published it.
 * This class is used to load the FastNbt library from Maven Central directly.
 */
public class LibsLoader extends LibraryManager
{
    private final Function<List<Path>, List<Path>> REMAPPER;
    private final URLClassLoaderHelper classLoader;
    private final YamlConfiguration pluginYAML;
    private final String libsKey;

    public LibsLoader(Main main)
    {
        this(main, "libraries-libby");
    }

    public LibsLoader(Main main, String libsKey)
    {
        this(main, libsKey, Repositories.Builder.DEFAULT);
    }

    public LibsLoader(Main plugin, String libsKey, Repositories repositories)
    {
        super(new LogAdapter()
        {
            @Override
            public void log(@NotNull LogLevel level, String message)
            {
                Logger logger = plugin.getLogger();
                switch (level)
                {
                    case DEBUG:
                        logger.log(Level.INFO, message);
                        break;
                    case INFO:
                        logger.log(Level.INFO, message);
                        break;
                    case WARN:
                        logger.log(Level.WARNING, message);
                        break;
                    case ERROR:
                        logger.log(Level.SEVERE, message);
                        break;
                }
            }

            @Override
            public void log(@NotNull LogLevel level, String message, Throwable throwable)
            {
                Logger logger = plugin.getLogger();
                switch (level)
                {
                    case DEBUG:
                        logger.log(Level.INFO, message, throwable);
                        break;
                    case INFO:
                        logger.log(Level.INFO, message, throwable);
                        break;
                    case WARN:
                        logger.log(Level.WARNING, message, throwable);
                        break;
                    case ERROR:
                        logger.log(Level.SEVERE, message, throwable);
                        break;
                }
            }
        }, plugin.getDataFolder().toPath(), "lib");
        classLoader = new URLClassLoaderHelper((URLClassLoader) plugin.getClass().getClassLoader(), this);
        this.pluginYAML = getPluginYaml(plugin);
        this.libsKey = libsKey;

        Function<List<Path>, List<Path>> remapper;
        try
        {
            Class<?> clazz = Class.forName("org.bukkit.plugin.java.LibraryLoader");
            Field remapperField = clazz.getDeclaredField("REMAPPER");

            //noinspection unchecked
            remapper = (Function<List<Path>, List<Path>>) remapperField.get(null);
        }
        catch (Exception e)
        {
            remapper = null;
        }

        REMAPPER = remapper;

        if(repositories.mavenLocal)
        {
            // Maven local repository, usually in `~/.m2/repository`
            addMavenLocal();
        }

        if(repositories.mavenCentral)
        {
            // Maven Central repository used by Spigot
            addRepository("https://maven-central.storage-download.googleapis.com/maven2/");
            // Official Maven Central repository
            addMavenCentral();
        }

        if(repositories.chineseMirror)
        {
            // Fallback to support chinese users
            addRepository("https://maven.aliyun.com/repository/central");
        }

        if(repositories.mavenRepositoriesUrls != null)
            repositories.mavenRepositoriesUrls.stream().filter(url -> url != null && !url.isEmpty()).forEach(url -> addRepository(url));
    }

    @Override
    protected void addToClasspath(@NotNull Path file)
    {
        classLoader.addToClasspath(file);
    }

    private Path remap(Path file)
    {
        return REMAPPER != null ? REMAPPER.apply(Collections.singletonList(file)).get(0) : file;
    }

    /**
     * Remap method by HSGamer: https://github.com/BetterGUI-MC/AnvilGUI/blob/0c1978df7195802628d89b5e3a32f465db583f75/src/main/java/me/hsgamer/bettergui/anvilgui/LibLoader.java
     */
    private void markJarAsToRemap(Path file, Path modifiedFile) throws IOException
    {
        Path tempJarPath = Files.createTempFile("tmp-", ".jar");

        try (JarFile jarFile = new JarFile(file.toFile());
             JarOutputStream tempJarOutputStream = new JarOutputStream(Files.newOutputStream(tempJarPath)))
        {
            // Copy the JAR file entries except the MANIFEST.MF into the new JAR file
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
            {
                JarEntry entry = entries.nextElement();
                if (JarFile.MANIFEST_NAME.equals(entry.getName()))
                    continue; // Skip the existing MANIFEST.MF

                try (InputStream entryInputStream = jarFile.getInputStream(entry))
                {
                    tempJarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = entryInputStream.read(buffer)) != -1)
                    {
                        tempJarOutputStream.write(buffer, 0, bytesRead);
                    }
                    tempJarOutputStream.closeEntry();
                }
            }

            // Load the MANIFEST.MF and add the remapping trigger property
            Manifest manifest = jarFile.getManifest();
            if (manifest == null)
                manifest = new Manifest();
            manifest.getMainAttributes().putValue("paperweight-mappings-namespace", "spigot");

            tempJarOutputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
            manifest.write(tempJarOutputStream);
            tempJarOutputStream.closeEntry();
        }

        Files.move(tempJarPath, modifiedFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public void loadLibrary(@NotNull Library library, boolean remap)
    {
        logger.info("Loading library " + library.getArtifactId() + (remap ? " (remapped)" : "") + "...");
        Path filePath = downloadLibrary(requireNonNull(library, "library"));
        if (library.resolveTransitiveDependencies())
            resolveTransitiveLibraries(library);

        if (remap)
        {
            try
            {
                Path destFilePath = filePath.getParent().resolve(FilenameUtils.getBaseName(filePath.getFileName().toString()) + "-modified.jar");
                if (!Files.exists(destFilePath))
                    markJarAsToRemap(filePath, destFilePath);
                filePath = remap(destFilePath);
            }
            catch (IOException e)
            {
                logger.error("Failed to remap library " + library.getArtifactId(), e);
                return;
            }
        }

        if (library.isIsolatedLoad())
            addToIsolatedClasspath(library, filePath);
        else
            addToClasspath(filePath);
    }

    public void loadAll()
    {
        List<String> libs = pluginYAML.getStringList(libsKey);
        for (String libString : libs)
        {
            String[] parts = libString.split(":");
            if (parts.length != 3)
            {
                logger.warn("Invalid library format: " + libString + ". Expected format: groupId:artifactId:version");
                continue;
            }

            boolean remapFlag = false;
            for (int i = 1; i < parts.length; i++)
            {
                if (parts[i].contains("--remap"))
                {
                    remapFlag = true;
                    break;
                }
            }

            // Cleanup parts form flags
            libString = libString.split(" ")[0];
            parts = libString.split(":");

            Library lib = Library.builder()
                    .groupId(parts[0])
                    .artifactId(parts[1])
                    .version(parts[2])
                    .resolveTransitiveDependencies(true)
                    .build();
            loadLibrary(lib, remapFlag);
        }
    }

    @NotNull
    private static YamlConfiguration getPluginYaml(Plugin plugin)
    {
        File file;
        try
        {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);
            file = (File) method.invoke(plugin);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException("Error accessing plugin JAR file.", e);
        }

        try
        {
            String str;
            try (JarFile jar = new JarFile(file))
            {
                JarEntry entry = jar.getJarEntry("plugin.yml");
                try (InputStream stream = jar.getInputStream(entry))
                {
                    str = (new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
                            .lines()
                            .collect(Collectors.joining("\n"))
                    ;
                }
            }
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(str);
            return config;
        }
        catch (InvalidConfigurationException | IOException e)
        {
            throw new RuntimeException("plugin.yml not found in the jar file.");
        }
    }

    public static class Repositories
    {
        public final boolean mavenLocal;
        public final boolean mavenCentral;
        public final boolean chineseMirror;
        public @Nullable Set<String> mavenRepositoriesUrls;

        private Repositories(Builder builder)
        {
            this.mavenLocal = builder.mavenLocal;
            this.mavenCentral = builder.mavenCentral;
            this.chineseMirror = builder.chineseMirror;
            this.mavenRepositoriesUrls = builder.mavenRepositoriesUrls;
        }

        public static Builder builder()
        {
            return new Builder();
        }

        public static class Builder
        {
            public static final Repositories DEFAULT = new Builder().build();
            private boolean mavenLocal = true;
            private boolean mavenCentral = true;
            private boolean chineseMirror = true;
            private @Nullable Set<String> mavenRepositoriesUrls = null;

            public Builder mavenLocal(boolean mavenLocal)
            {
                this.mavenLocal = mavenLocal;
                return this;
            }

            public Builder mavenCentral(boolean mavenCentral)
            {
                this.mavenCentral = mavenCentral;
                return this;
            }

            public Builder chineseMirror(boolean chineseMirror)
            {
                this.chineseMirror = chineseMirror;
                return this;
            }

            public Builder mavenRepositoriesUrls(@Nullable Set<String> mavenRepositoriesUrls)
            {
                this.mavenRepositoriesUrls = mavenRepositoriesUrls;
                return this;
            }

            public Repositories build()
            {
                return new Repositories(this);
            }
        }
    }
}