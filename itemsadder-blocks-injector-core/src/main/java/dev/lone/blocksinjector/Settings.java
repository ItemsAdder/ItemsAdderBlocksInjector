package dev.lone.blocksinjector;

import org.bukkit.plugin.Plugin;

public class Settings
{
    public static boolean debug = false;

    public static void init(Plugin plugin)
    {
        Settings.debug = plugin.getConfig().getBoolean("debug", false);
    }
}
