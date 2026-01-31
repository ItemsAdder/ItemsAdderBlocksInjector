package dev.lone.blocksinjector;

import dev.lone.blocksinjector.customblocks.BlockInjector;
import org.bukkit.plugin.Plugin;

public class Nms {

    public static BlockInjector getBlockInjector(String version) {
        return switch (version) {
            case "1.21.11" -> new dev.lone.blocksinjector.customblocks.v1_21_11.BlockInjector();
            case "1.21.9", "1.21.10" -> new dev.lone.blocksinjector.customblocks.v1_21_9.BlockInjector();
            case "1.21.7", "1.21.8" -> new dev.lone.blocksinjector.customblocks.v1_21_7.BlockInjector();
            case "1.21.6" -> new dev.lone.blocksinjector.customblocks.v1_21_6.BlockInjector();
            case "1.21.5" -> new dev.lone.blocksinjector.customblocks.v1_21_5.BlockInjector();
            case "1.21.4" -> new dev.lone.blocksinjector.customblocks.v1_21_4.BlockInjector();
            case "1.21.3" -> new dev.lone.blocksinjector.customblocks.v1_21_3.BlockInjector();
            // Paper 1.21.2 isn't downloadable
            default -> throw new IllegalArgumentException("Unsupported version: " + version);
        };
    }

    public static void createPacketListener(String version, Plugin plugin) {
        switch (version) {
            case "1.21.11" -> new dev.lone.blocksinjector.customblocks.v1_21_11.PacketListener(plugin);
            case "1.21.9", "1.21.10" -> new dev.lone.blocksinjector.customblocks.v1_21_9.PacketListener(plugin);
            case "1.21.7", "1.21.8" -> new dev.lone.blocksinjector.customblocks.v1_21_7.PacketListener(plugin);
            case "1.21.6" -> new dev.lone.blocksinjector.customblocks.v1_21_6.PacketListener(plugin);
            case "1.21.5" -> new dev.lone.blocksinjector.customblocks.v1_21_5.PacketListener(plugin);
            case "1.21.4" -> new dev.lone.blocksinjector.customblocks.v1_21_4.PacketListener(plugin);
            case "1.21.3" -> new dev.lone.blocksinjector.customblocks.v1_21_3.PacketListener(plugin);
            // Paper 1.21.2 isn't downloadable
            default -> throw new IllegalArgumentException("Unsupported version: " + version);
        }
    }

}
