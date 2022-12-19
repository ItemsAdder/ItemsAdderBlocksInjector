package dev.lone.blocksinjector.custom_blocks.nms;

import dev.lone.blocksinjector.Main;
import org.bukkit.Bukkit;
import dev.lone.blocksinjector.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class Nms
{
    public static Field getField(Class<?> clazz, Class<?> type, int index)
    {
        int i = 0;
        for (Field field : clazz.getDeclaredFields())
        {
            if (field.getType() == type)
            {
                if(index == i)
                {
                    field.setAccessible(true);
                    return field;
                }
                i++;
            }
        }
        return null;
    }

    /**
     * Gets a suitable implementaion for the current Minecraft server version.
     *
     * @param implClazz   the interface class which every implementation implements.
     * @param ignoreError If loading errors should be ignored, for example if no implementation exists. This is hacky but can
     *                    be useful in some cases.
     * @return the correct implementation.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T findImplementation(Class<T> implClazz, boolean ignoreError, Object ...args)
    {
        String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];

        try
        {
            Class<?> implClass = Class.forName(implClazz.getPackage().getName() + "." + nmsVersion);
            try
            {
                //To handle nms implementations which have an arg in constructor
                return (T) implClass.getDeclaredConstructor(implClazz).newInstance(args);
            }
            catch (NoSuchMethodException e)
            {
                return (T) implClass.getDeclaredConstructor().newInstance();
            }
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            if (ignoreError)
                return null;

            Bukkit.getLogger().severe("Error getting implementation for " + implClazz + " - NMS " + nmsVersion);
            e.printStackTrace();

            Bukkit.getPluginManager().disablePlugin(Main.inst); // Is this needed?
            Bukkit.shutdown();
        }
        return null;
    }
}
