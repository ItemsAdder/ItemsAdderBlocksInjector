package dev.lone.blocksinjector;

import com.comphenix.protocol.ProtocolLibrary;
import io.papermc.paper.ServerBuildInfo;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractMain extends JavaPlugin {

    @Override
    public void onEnable() {
        createPacketListener(ServerBuildInfo.buildInfo().minecraftVersionId());
    }

    abstract void createPacketListener(String version);

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }

}
