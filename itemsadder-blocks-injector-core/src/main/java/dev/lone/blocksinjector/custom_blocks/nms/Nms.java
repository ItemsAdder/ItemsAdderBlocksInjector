package dev.lone.blocksinjector.custom_blocks.nms;

import beer.devs.fastnbt.nms.Version;
import dev.lone.blocksinjector.Main;
import org.bukkit.Bukkit;
import dev.lone.blocksinjector.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Nms
{
    @Nullable
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
     * Gets a suitable implementation for the current Minecraft server version.
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
        String nmsVersion = Version.get().name();

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

    public enum MethodVisibility
    {
        PUBLIC,
        PROTECTED,
        PRIVATE,
        PACKAGE_PRIVATE
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?> ... argsTypes)
    {
        try
        {
            Method method = clazz.getDeclaredMethod(name, argsTypes);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    public static Method findMethod(Class<Void> returnType, Class<?> clazz, MethodVisibility type, Class<?> ... argsTypes)
    {
        for (Method method : clazz.getDeclaredMethods())
        {
            switch (type)
            {
                case PUBLIC:
                    if (!Modifier.isPublic(method.getModifiers()))
                        continue;
                    break;
                case PROTECTED:
                    if (!Modifier.isProtected(method.getModifiers()))
                        continue;
                    break;
                case PRIVATE:
                    if (!Modifier.isPrivate(method.getModifiers()))
                        continue;
                    break;
                case PACKAGE_PRIVATE:
                    if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers()) || Modifier.isPrivate(method.getModifiers()))
                        continue;
                    break;
            }

            if (method.getReturnType() != returnType)
                continue;

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == argsTypes.length)
            {
                boolean match = true;
                for (int i = 0; i < parameterTypes.length; i++)
                {
                    if (parameterTypes[i] != argsTypes[i])
                    {
                        match = false;
                        break;
                    }
                }
                if (match)
                {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }
}
