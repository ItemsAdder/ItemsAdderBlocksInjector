package dev.lone.blocksinjector.customblocks.v1_21_11;

import dev.lone.blocksinjector.NMSCustomBlock;
import net.kyori.adventure.key.Key;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BlockInjector implements dev.lone.blocksinjector.customblocks.BlockInjector {

    // NMS injection
    @SuppressWarnings("unchecked")
    private static final MappedRegistry<Block> REGISTRY = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;

    @Override
    public void injectBlock(
            @NotNull CustomBlocKInfo customBlocKInfo
    ) throws Exception {
        // Make sure MC classes are loaded
        Objects.requireNonNull(net.minecraft.world.level.block.Blocks.AIR);
        Objects.requireNonNull(net.minecraft.world.item.Items.AIR);
        Objects.requireNonNull(CraftMagicNumbers.INSTANCE);

        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.parse(customBlocKInfo.key()));

        BlockBehaviour.Properties properties = (switch (customBlocKInfo.type()) {
            case REAL, REAL_NOTE -> BlockBehaviour.Properties.of().strength(0.8f);
            case REAL_TRANSPARENT -> BlockBehaviour.Properties.of().noOcclusion().strength(0.8f);
            case REAL_WIRE -> BlockBehaviour.Properties.of().noCollision().strength(0.8f);
        }).setId(key);

        NMSCustomBlock block = new NMSCustomBlock(properties, customBlocKInfo.id(), Key.key(customBlocKInfo.key()));

        REGISTRY.createIntrusiveHolder(block);
        REGISTRY.register(key, block, RegistrationInfo.BUILT_IN);

        Block.BLOCK_STATE_REGISTRY.add(block.defaultBlockState());

        // Fixes some bugs with Bukkit and plugins
        Reflection.setBlockMaterial(block, switch (customBlocKInfo.type()) {
            case REAL, REAL_NOTE, REAL_TRANSPARENT -> Material.COBBLESTONE;
            case REAL_WIRE -> Material.TRIPWIRE;
        });
    }

}
