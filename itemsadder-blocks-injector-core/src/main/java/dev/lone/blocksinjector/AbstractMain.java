package dev.lone.blocksinjector;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.customblocks.BlockInjector;
import io.papermc.paper.ServerBuildInfo;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractMain extends JavaPlugin {

    @Override
    public void onEnable() {
        createPacketListener(ServerBuildInfo.buildInfo().minecraftVersionId());
        if (Bukkit.getPluginManager().isPluginEnabled("Iris")) {
            IrisHook.injectFromBlockInjector();
        }
        BlockInjector.BLOCKS.clear();
    }

    abstract void createPacketListener(String version);

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }

}
