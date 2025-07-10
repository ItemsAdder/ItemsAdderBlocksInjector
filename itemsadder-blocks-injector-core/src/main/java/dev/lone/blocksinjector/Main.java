package dev.lone.blocksinjector;

import com.comphenix.protocol.ProtocolLibrary;
import com.viaversion.viaversion.ViaVersionPlugin;
import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.CustomBlocksInjector;
import dev.lone.blocksinjector.custom_blocks.nms.packetlistener.DigPacketListener;
import dev.lone.itemsadder.utils.Msg;
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
        DigPacketListener.register();
    }

    @Override
    public void onEnable()
    {
        Bukkit.getPluginManager().registerEvents(this, this);

        if(ViaVersionPlugin.getInstance().getApi().apiVersion() < 22)
        {
            Bukkit.getLogger().severe("Using outdated ViaVersion. Please update to 4.9.3 or greater.");
            Bukkit.getPluginManager().disablePlugin(this); // Is this needed?
            Bukkit.shutdown();
            return;
        }

        if(Bukkit.getPluginManager().isPluginEnabled("LoneLibs"))
            Msg.warn("LoneLibs is enabled. This plugin is not needed anymore and it's recommended to remove it (if you are not using it for other plugins).");
    }

    @Override
    public void onDisable()
    {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }

    //TODO: Maybe it's worth adding custom blocks to the vanilla command? since they now will be correctly
    // resolved by the internal Minecract code.
    // Idk if it's actually useful.
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
//    private void tab(TabCompleteEvent e)
//    {
//        if(e.getBuffer().startsWith("/setblock"))
//        {
//
//        }
//    }
}
