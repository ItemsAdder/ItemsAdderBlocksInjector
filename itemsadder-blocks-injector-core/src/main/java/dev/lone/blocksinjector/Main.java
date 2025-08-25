package dev.lone.blocksinjector;

import com.comphenix.protocol.ProtocolLibrary;
import com.viaversion.viaversion.api.Via;
import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.CustomBlocksInjector;
import dev.lone.blocksinjector.custom_blocks.nms.packetlistener.LegacyDigPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener
{
    public static Main inst;

    @Override
    public void onLoad()
    {
        inst = this;

        new LibsLoader(this).loadAll();

        Settings.init(inst);


        try
        {
            CustomBlocksInjector.initNms();
        }
        catch (Throwable e)
        {
            getLogger().severe("Failed to initialize NMS. Please check the console for more information.");
            e.printStackTrace();
            Bukkit.shutdown();
            return;
        }

        CustomBlocksInjector.inst.loadFromCache();
        CustomBlocksInjector.inst.registerListener();

        if(isItemsAdderPre4_0_12())
            LegacyDigPacketListener.register();
    }

    private boolean isItemsAdderPre4_0_12()
    {
        // Check if ItemsAdder version is lower than 4.0.12
        String itemsAdderVersion = Bukkit.getPluginManager().getPlugin("ItemsAdder").getDescription().getVersion();
        String[] versionParts = itemsAdderVersion.split("\\.");
        int major = Integer.parseInt(versionParts[0]);
        int minor = Integer.parseInt(versionParts[1]);
        int patch = 0;

        if (versionParts.length > 2)
        {
            String patchStr = versionParts[2].split("[^0-9]")[0]; // Only the numeric part
            patch = Integer.parseInt(patchStr);
        }

        return major < 4 || (major == 4 && minor == 0 && patch < 12);
    }

    @Override
    public void onEnable()
    {
        Bukkit.getPluginManager().registerEvents(this, this);

        if(Via.getAPI().apiVersion() < 22)
        {
            Bukkit.getLogger().severe("Using outdated ViaVersion. Please update to 4.9.3 or greater.");
            Bukkit.getPluginManager().disablePlugin(this); // Is this needed?
            Bukkit.shutdown();
            return;
        }

        if(Bukkit.getPluginManager().isPluginEnabled("LoneLibs"))
            Bukkit.getLogger().warning("LoneLibs is enabled. This plugin is not needed anymore and it's recommended to remove it (if you are not using it for other plugins).");
    }

    @Override
    public void onDisable()
    {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }
}
