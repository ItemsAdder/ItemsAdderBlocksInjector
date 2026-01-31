package dev.lone.blocksinjector;

import dev.lone.blocksinjector.customblocks.BlockInjector;
import org.bukkit.plugin.Plugin;

public class Nms {

    public static BlockInjector getBlockInjector(String version) {
        return switch (version) {
            case "1.21.11" -> new dev.lone.blocksinjector.customblocks.v1_21_11.BlockInjector();
            case "1.21.9", "1.21.10" -> new dev.lone.blocksinjector.customblocks.v1_21_9.BlockInjector();
            default -> throw new IllegalArgumentException("Unsupported version: " + version);
        };
    }

    public static void createPacketListener(String version, Plugin plugin) {
        switch (version) {
            case "1.21.11" -> new dev.lone.blocksinjector.customblocks.v1_21_11.PacketListener(plugin);
            case "1.21.9", "1.21.10" -> new dev.lone.blocksinjector.customblocks.v1_21_9.PacketListener(plugin);
            default -> throw new IllegalArgumentException("Unsupported version: " + version);
        }
    }

}
