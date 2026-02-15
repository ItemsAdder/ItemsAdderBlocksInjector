package dev.lone.blocksinjector.customblocks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public interface BlockInjector {

    void injectBlock(@NotNull CustomBlocKInfo customBlocKInfo) throws Exception;

    default void loadFromCache(ComponentLogger logger) {
        File storageFolder = new File("plugins/ItemsAdder/storage");
        if (storageFolder.exists()) {

            ObjectArrayList<CustomBlocKInfo> namespacedBlocks = new ObjectArrayList<>();
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_blocks_ids_cache", CustomBlocKInfo.Type.REAL));
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_blocks_note_ids_cache", CustomBlocKInfo.Type.REAL_NOTE));
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_transparent_blocks_ids_cache", CustomBlocKInfo.Type.REAL_TRANSPARENT));
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_wire_ids_cache", CustomBlocKInfo.Type.REAL_WIRE));

            try {
                for (CustomBlocKInfo customBlocKInfo : namespacedBlocks) {
                    injectBlock(customBlocKInfo);
                }
            } catch (Exception e) {
                logger.error("Error loading custom blocks from ItemsAdder cache files.", e);
                System.exit(1);
            }
        } else {
            logger.error("No ItemsAdder/storage folder found. ItemsAdderBlocksInjector won't do anything.");
        }
    }

    default ObjectArrayList<CustomBlocKInfo> loadCacheFile(File storageFolder, String cacheFileName, CustomBlocKInfo.Type type) {
        ObjectArrayList<CustomBlocKInfo> list = new ObjectArrayList<>();
        File file = new File(storageFolder, cacheFileName + ".yml");
        if (file.exists()) {
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            for (String key : configuration.getKeys(false)) {
                int id = configuration.getInt(key);
                list.add(new CustomBlocKInfo(key, id, type));
            }
        }
        return list;
    }

    record CustomBlocKInfo(String key, int id, Type type) {
        public enum Type {
            REAL, REAL_NOTE, REAL_TRANSPARENT, REAL_WIRE
        }
    }

}
