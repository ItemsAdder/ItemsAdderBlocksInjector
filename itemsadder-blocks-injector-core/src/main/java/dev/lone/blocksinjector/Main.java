package dev.lone.blocksinjector;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.CustomBlocksInjector;
import dev.lone.blocksinjector.custom_blocks.nms.packetlistener.DigPacketListener;
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

        Settings.init(inst);

        CustomBlocksInjector.initNms();
        CustomBlocksInjector.inst.loadFromCache();

        CustomBlocksInjector.inst.registerListener();
        DigPacketListener.register();
    }

    @Override
    public void onEnable()
    {
        Bukkit.getPluginManager().registerEvents(this, this);
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
