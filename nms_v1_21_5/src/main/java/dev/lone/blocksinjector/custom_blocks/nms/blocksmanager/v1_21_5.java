package dev.lone.blocksinjector.custom_blocks.nms.blocksmanager;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.IrisHook;
import dev.lone.blocksinjector.Main;
import dev.lone.blocksinjector.Settings;
import dev.lone.blocksinjector.annotations.Nullable;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.blocksinjector.custom_blocks.nms.Nms;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unused")
public class v1_21_5 extends CustomBlocksInjector<Block, BlockState, ClientboundLevelChunkPacketData>
{
    public v1_21_5()
    {
        try
        {
            field_BLOCK_MATERIAL = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            field_BLOCK_MATERIAL.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }

        BUFFER_FIELD = Arrays.stream(ClientboundLevelChunkPacketData.class.getDeclaredFields())
                .filter(f -> f.getType() == byte[].class)
                .peek(f -> f.setAccessible(true))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No byte[] field found in ClientboundLevelChunkPacketData"));
    }

    @Override
    public void registerListener()
    {
        if (packet == null)
        {
            packet = new dev.lone.blocksinjector.custom_blocks.nms.packetlistener.v1_21_5(Main.inst);
            ProtocolLibrary.getProtocolManager().addPacketListener(packet);
        }
    }

    @Override
    void injectBlocks(HashMap<CachedCustomBlockInfo, Integer> customBlocks)
    {
        if (Settings.debug)
            Main.inst.getLogger().info("Injecting " + customBlocks.size() + " blocks...");

        Field field_unregisteredIntrusiveHolders = Nms.getField(MappedRegistry.class, Map.class, 5);
        if(field_unregisteredIntrusiveHolders == null)
            throw new RuntimeException("Error injecting blocks: field_unregisteredIntrusiveHolders is null");

        Field field_frozen;
        MappedRegistry<Block> registry;
        try
        {
            registry = ((MappedRegistry) BuiltInRegistries.BLOCK);
            field_frozen = Nms.getField(MappedRegistry.class, boolean.class, 0);
            if(field_frozen == null)
                throw new RuntimeException("Error injecting blocks: field_frozen is null");
            field_frozen.setAccessible(true);
            // Unfreeze the registry to allow modifications
            field_frozen.set(registry, false);
        }
        catch (SecurityException | IllegalArgumentException | IllegalAccessException | NullPointerException e)
        {
            Main.inst.getLogger().warning("Error injecting blocks: " + e.getMessage());
            throw new RuntimeException(e);
        }

        customBlocks.forEach((cached, integer) -> {
            try
            {
                Block internalBlock = isBlockAlreadyRegistered(cached);
                if (internalBlock != null)
                {
                    if (Settings.debug)
                        Main.inst.getLogger().warning("Block '" + internalBlock.getDescriptionId() + "' already registered, skipping.");
                }
                else
                {
                    field_unregisteredIntrusiveHolders.setAccessible(true);
                    field_unregisteredIntrusiveHolders.set(registry, new IdentityHashMap<>());

                    // Use similar blocks as base (similar breaking speed, server-side collisions, etc.)
                    BlockBehaviour.Properties properties = switch (cached.type)
                    {
                        case REAL, REAL_NOTE -> BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK);
                        case REAL_TRANSPARENT -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_ROD).lightLevel(value -> 0);
                        case REAL_WIRE -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS);
                        default -> throw new RuntimeException("Not implemented!");
                    };

                    ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(cached.namespace, cached.key);
                    ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, loc);
                    properties.setId(key);

                    internalBlock = new Block(properties);

                    registry.createIntrusiveHolder(internalBlock);
                    Holder<Block> holder = registry.register(key, internalBlock, RegistrationInfo.BUILT_IN);

                    Set<TagKey<Block>> tags = new HashSet<>();
                    Holder.direct(internalBlock).tags().forEach(tags::add);

                    Method method_bindTags = Nms.findMethod(Holder.Reference.class, "bindTags", Collection.class);
                    if (method_bindTags == null)
                        method_bindTags = Nms.findMethod(void.class, Holder.Reference.class, Nms.MethodVisibility.PACKAGE_PRIVATE, Collection.class);

                    if (method_bindTags == null)
                        throw new RuntimeException("Error injecting blocks: method_bindTags is null");

                    method_bindTags.invoke(holder, tags);

                    // Reset unregisteredIntrusiveHolders to null to prevent issues
                    field_unregisteredIntrusiveHolders.set(registry, null);

                    Block.BLOCK_STATE_REGISTRY.add(internalBlock.defaultBlockState());

                    //<editor-fold desc="Inject the block into the Bukkit lookup data structures to avoid incompatibilities with plugins">
                    try
                    {
                        HashMap<Block, Material> BLOCK_MATERIAL = (HashMap<Block, Material>) field_BLOCK_MATERIAL.get(null);
                        BLOCK_MATERIAL.put(internalBlock, Material.COBBLESTONE); // Dummy Bukkit Material

                        if (Settings.debug)
                            Main.inst.getLogger().info("Injected block into Bukkit lookup: " + cached.getNamespacedId());
                    }
                    catch (IllegalAccessException e)
                    {
                        Main.inst.getLogger().warning("Error injecting block into Bukkit lookup: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                    //</editor-fold>

                    REGISTRY.add(internalBlock);

                    if (Settings.debug)
                        Main.inst.getLogger().info("Injected block into Minecraft Registry.BLOCK: " + cached.getNamespacedId());
                }

                registeredBlocks.put(internalBlock.getDescriptionId(), cached);
            }
            catch (Throwable e)
            {
                Main.inst.getLogger().warning("Error registering block '" + cached.getNamespacedId() + "'.");
                throw new RuntimeException(e);
            }
        });

        try
        {
            field_frozen.set(registry, true);
        }
        catch (IllegalAccessException e)
        {
            Main.inst.getLogger().warning("Failed to freeze registry: " + e.getMessage());
            throw new RuntimeException(e);
        }

        if (Bukkit.getPluginManager().getPlugin("Iris") != null)
            IrisHook.inject(customBlocks);

        if (Settings.debug)
            Main.inst.getLogger().info("Finished injecting blocks");
    }

    @Override
    public BlockState calculateSpoofedNmsBlockFromItemsAdderCachedId(CachedCustomBlockInfo cachedBlock)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) getItemsAdderBlockDataByInternalId(cachedBlock.itemsAdderId);
        return bukkitData.getState();
    }

    @Override
    public int calculateSpoofedNmsBlockIdFromCachedItemsAdderId(int itemsAdderId)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) getItemsAdderBlockDataByInternalId(itemsAdderId);
        return Block.BLOCK_STATE_REGISTRY.getId(bukkitData.getState());
    }

    @Override
    @Nullable
    Block isBlockAlreadyRegistered(CachedCustomBlockInfo cached) // Is this function even making sense?
    {
        try
        {
            Bukkit.createBlockData(cached.getNamespacedId());
        }
        catch (IllegalArgumentException e) // Block not registered
        {
            // TODO: recode this shit
            for (BlockState state : Block.BLOCK_STATE_REGISTRY)
            {
                Block internalBlock = state.getBlock();
                if (internalBlock.getDescriptionId().equals("block." + cached.namespace + "." + cached.key))
                    return internalBlock;
            }
        }
        return null;
    }

    @Override
    public String getDescriptionId(org.bukkit.block.Block bukkitBlock)
    {
        Location blockLoc = bukkitBlock.getLocation();
        return ((CraftBlock) bukkitBlock).getHandle().getBlockState(new BlockPos(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ())).getBlock().getDescriptionId();
    }

    @Deprecated
    @Override
    void unfreezeRegistry()
    {
        // TODO remove
    }
}
