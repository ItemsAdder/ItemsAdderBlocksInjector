package dev.lone.blocksinjector.customblocks.v1_21_5;

import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.SingleValuePalette;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

import java.lang.reflect.Field;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Reflection {

    private static final Field STATES_FIELD;
    private static final Field BUFFER_FIELD;
    private static final Field BLOCK_MATERIAL_FIELD;
    private static final Field MATERIAL_BLOCK_FIELD;
    private static final Field SINGLE_VALUE_PALETTE_VALUE_FIELD;

    static {
        try {
            STATES_FIELD = ClientboundSectionBlocksUpdatePacket.class.getDeclaredField("states");
            STATES_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize STATES_FIELD", e);
        }
        try {
            BUFFER_FIELD = ClientboundLevelChunkPacketData.class.getDeclaredField("buffer");
            BUFFER_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BUFFER_FIELD", e);
        }
        try {
            BLOCK_MATERIAL_FIELD = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            BLOCK_MATERIAL_FIELD.setAccessible(true);
            MATERIAL_BLOCK_FIELD = CraftMagicNumbers.class.getDeclaredField("MATERIAL_BLOCK");
            MATERIAL_BLOCK_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Bukkit material fields", e);
        }
        try {
            SINGLE_VALUE_PALETTE_VALUE_FIELD = SingleValuePalette.class.getDeclaredField("value");
            SINGLE_VALUE_PALETTE_VALUE_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SINGLE_VALUE_PALETTE_VALUE_FIELD", e);
        }
    }

    public static BlockState[] getBlockStates(ClientboundSectionBlocksUpdatePacket packet) throws Exception {
        return (BlockState[]) STATES_FIELD.get(packet);
    }

    public static void setBlockStates(ClientboundSectionBlocksUpdatePacket packet, BlockState[] states) throws Exception {
        STATES_FIELD.set(packet, states);
    }

    public static void setBuffer(ClientboundLevelChunkPacketData packet, byte[] buffer) throws Exception {
        BUFFER_FIELD.set(packet, buffer);
    }

    public static void setBlockMaterial(Block block, Material material) throws Exception {
        ((Map<Block, Material>) BLOCK_MATERIAL_FIELD.get(CraftMagicNumbers.INSTANCE)).put(block, material);
        ((Map<Material, Block>) MATERIAL_BLOCK_FIELD.get(CraftMagicNumbers.INSTANCE)).put(material, block);
    }

    public static <T> void setValue(SingleValuePalette<T> palette, T value) throws Exception {
        SINGLE_VALUE_PALETTE_VALUE_FIELD.set(palette, value);
    }
}
