package dev.lone.blocksinjector.customblocks.v1_21_11;

import dev.lone.blocksinjector.IrisHook;
import dev.lone.blocksinjector.NMSCustomBlock;
import dev.lone.itemsadder.api.ItemsAdder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.key.Key;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

public class BlockInjector implements dev.lone.blocksinjector.customblocks.BlockInjector {

    @Override
    public void injectBlocks(@NotNull ObjectArrayList<CustomBlocKInfo> namespacedBlocks) throws Exception {
        for (CustomBlocKInfo customBlocKInfo : namespacedBlocks) {
            CraftBlockData blockData = (CraftBlockData) ItemsAdder.Advanced.getBlockDataByInternalId(customBlocKInfo.id());
            if (blockData == null) throw new RuntimeException("Failed to get block data for " + customBlocKInfo.id());
            BlockState blockState = blockData.getState();
            injectBlock(customBlocKInfo.key(), blockState);
        }
    }

    // NMS injection
    @SuppressWarnings("unchecked")
    private static final MappedRegistry<Block> REGISTRY = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;

    public static void injectBlock(
            @NotNull String id,
            @NotNull BlockState clientBlockState
    ) throws Exception {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.parse(id));

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(clientBlockState.getBlock())
                .setId(key);

        NMSCustomBlock block = new NMSCustomBlock(properties, clientBlockState, Key.key(id));

        REGISTRY.createIntrusiveHolder(block);
        REGISTRY.register(key, block, RegistrationInfo.BUILT_IN);

        Block.BLOCK_STATE_REGISTRY.add(block.defaultBlockState());

        // Fixes some bugs with Bukkit and plugins
        setBlockMaterial(block, clientBlockState.getBukkitMaterial());

        if (Bukkit.getPluginManager().getPlugin("Iris") != null)
            IrisHook.inject(block);
    }

    // Bukkit Material Reflection
    private static final Field BLOCK_MATERIAL_FIELD;
    private static final Field MATERIAL_BLOCK_FIELD;

    static {
        try {
            BLOCK_MATERIAL_FIELD = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            BLOCK_MATERIAL_FIELD.setAccessible(true);
            MATERIAL_BLOCK_FIELD = CraftMagicNumbers.class.getDeclaredField("MATERIAL_BLOCK");
            MATERIAL_BLOCK_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Bukkit material fields", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void setBlockMaterial(Block block, Material material) throws Exception {
        ((Map<Block, Material>) BLOCK_MATERIAL_FIELD.get(CraftMagicNumbers.INSTANCE)).put(block, material);
        ((Map<Material, Block>) MATERIAL_BLOCK_FIELD.get(CraftMagicNumbers.INSTANCE)).put(material, block);
    }
}
