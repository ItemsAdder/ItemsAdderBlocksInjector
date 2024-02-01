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

        if(ViaVersionPlugin.getInstance().getApi().apiVersion() < 22)
        {
            Bukkit.getLogger().severe("Using outdated ViaVersion. Please update to 4.9.3 or greater.");
            Bukkit.getPluginManager().disablePlugin(this); // Is this needed?
            Bukkit.shutdown();
            return;
        }

        //<editor-fold desc="Check LoneLibs compatibility">
        {
            boolean compatible = false;
            try
            {
                //noinspection UnnecessaryFullyQualifiedName
                dev.lone.LoneLibs.LoneLibs.CompareVersionResult compareVersionResult = dev.lone.LoneLibs.LoneLibs.compareVersion("1.0.45");
                //noinspection UnnecessaryFullyQualifiedName
                compatible = compareVersionResult == dev.lone.LoneLibs.LoneLibs.CompareVersionResult.INSTALLED_IS_SAME
                        || compareVersionResult == dev.lone.LoneLibs.LoneLibs.CompareVersionResult.INSTALLED_IS_NEWER;
            }
            catch (Throwable ignored) {}
            if (!compatible)
            {
                Msg.error("Please update LoneLibs! https://a.devs.beer/ia-install-lonelibs");
                Bukkit.getPluginManager().disablePlugin(this);
                Bukkit.shutdown();
                return;
            }
        }
        //</editor-fold>
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
