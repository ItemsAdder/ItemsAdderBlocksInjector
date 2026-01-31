package dev.lone.blocksinjector.customblocks.v1_21;

import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.SingleValuePalette;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Reflection {

    private static final Field STATES_FIELD;
    private static final Field BUFFER_FIELD;
    private static final Field BLOCK_MATERIAL_FIELD;
    private static final Field MATERIAL_BLOCK_FIELD;
    private static final Field DATA_PALETTE_FIELD;
    private static final Field LINEAR_PALETTE_VALUES_FIELD;
    private static final Field HASHMAP_PALETTE_VALUES_FIELD;
    private static final Field CRUDE_INCREMENTAL_INT_IDENTITY_HASH_BI_MAP_BYID_FIELD;
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
            DATA_PALETTE_FIELD = Arrays.stream(PalettedContainer.class.getDeclaredClasses())
                    .filter(clazz -> clazz.getSimpleName().equals("Data"))
                    .findFirst()
                    .orElseThrow(() -> new ClassNotFoundException("Could not find PalettedContainer$Data"))
                    .getDeclaredField("palette");
            DATA_PALETTE_FIELD.setAccessible(true);

            LINEAR_PALETTE_VALUES_FIELD = net.minecraft.world.level.chunk.LinearPalette.class.getDeclaredField("values");
            LINEAR_PALETTE_VALUES_FIELD.setAccessible(true);

            HASHMAP_PALETTE_VALUES_FIELD = net.minecraft.world.level.chunk.HashMapPalette.class.getDeclaredField("values");
            HASHMAP_PALETTE_VALUES_FIELD.setAccessible(true);

            CRUDE_INCREMENTAL_INT_IDENTITY_HASH_BI_MAP_BYID_FIELD = net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap.class.getDeclaredField("byId");
            CRUDE_INCREMENTAL_INT_IDENTITY_HASH_BI_MAP_BYID_FIELD.setAccessible(true);

            SINGLE_VALUE_PALETTE_VALUE_FIELD = net.minecraft.world.level.chunk.SingleValuePalette.class.getDeclaredField("value");
            SINGLE_VALUE_PALETTE_VALUE_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Palette reflection fields", e);
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
        ((Map<Block, Material>) BLOCK_MATERIAL_FIELD.get(null)).put(block, material);
        ((Map<Material, Block>) MATERIAL_BLOCK_FIELD.get(null)).put(material, block);
    }

    public static @NotNull Object[] getPaletteValues(@NotNull PalettedContainer<?> container) throws Exception {
        Object data = container.data;
        Palette<?> palette = (Palette<?>) DATA_PALETTE_FIELD.get(data);

        if (palette instanceof net.minecraft.world.level.chunk.LinearPalette<?>) {
            return (Object[]) LINEAR_PALETTE_VALUES_FIELD.get(palette);
        } else if (palette instanceof net.minecraft.world.level.chunk.HashMapPalette<?>) {
            return (Object[]) CRUDE_INCREMENTAL_INT_IDENTITY_HASH_BI_MAP_BYID_FIELD.get(HASHMAP_PALETTE_VALUES_FIELD.get(palette));
        } else if (palette instanceof net.minecraft.world.level.chunk.SingleValuePalette<?>) {
            return new Object[]{SINGLE_VALUE_PALETTE_VALUE_FIELD.get(palette)};
        }
        throw new IllegalStateException("Unknown palette type: " + palette.getClass());
    }

    public static void setPaletteValues(@NotNull PalettedContainer<?> container, @NotNull Object[] values) throws Exception {
        Object data = container.data;
        Palette<?> palette = (Palette<?>) DATA_PALETTE_FIELD.get(data);

        switch (palette) {
            case SingleValuePalette<?> singleValuePalette ->
                    SINGLE_VALUE_PALETTE_VALUE_FIELD.set(singleValuePalette, values[0]);
            case net.minecraft.world.level.chunk.LinearPalette<?> linearPalette ->
                    LINEAR_PALETTE_VALUES_FIELD.set(linearPalette, values);
            case net.minecraft.world.level.chunk.HashMapPalette<?> hashMapPalette ->
                    CRUDE_INCREMENTAL_INT_IDENTITY_HASH_BI_MAP_BYID_FIELD.set(HASHMAP_PALETTE_VALUES_FIELD.get(hashMapPalette), values);
            default -> throw new IllegalStateException("Unknown palette type: " + palette.getClass());
        }
    }

}
