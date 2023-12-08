package dev.lone.blocksinjector.custom_blocks.nms.blocksmanager;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.IrisHook;
import dev.lone.blocksinjector.Main;
import dev.lone.blocksinjector.Settings;
import dev.lone.blocksinjector.annotations.Nullable;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.blocksinjector.custom_blocks.nms.Nms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftMagicNumbers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class v1_20_R2 extends CustomBlocksInjector<Block, BlockState, ClientboundLevelChunkPacketData>
{
    public v1_20_R2()
    {
        try
        {
            field_BLOCK_MATERIAL = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            field_BLOCK_MATERIAL.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        BUFFER_FIELD = ClientboundLevelChunkPacketData.class.getDeclaredFields()[2];
        BUFFER_FIELD.setAccessible(true);
    }

    @Override
    public void registerListener()
    {
        if(packet == null)
        {
            packet = new dev.lone.blocksinjector.custom_blocks.nms.packetlistener.v1_20_R2(Main.inst);
            ProtocolLibrary.getProtocolManager().addPacketListener(packet);
        }
    }

    @Override
    void injectBlocks(HashMap<CachedCustomBlockInfo, Integer> customBlocks)
    {
        if(Settings.debug)
            Bukkit.getLogger().info("Injecting " + customBlocks.size() + " blocks...");

        unfreezeRegistry();
        customBlocks.forEach((cached, integer) -> {

            Block internalBlock = isBlockAlreadyRegistered(cached);
            if(internalBlock != null)
            {
                if(Settings.debug)
                    Bukkit.getLogger().warning("Block '" + internalBlock.getDescriptionId() + "' already registered, skipping.");
            }
            else
            {
                try
                {
                    BlockBehaviour.Properties properties;
                    // Use similar blocks as base (similar breaking speed, server-side collisions, etc.)
                    switch (cached.type)
                    {
                        case REAL:
                        case REAL_NOTE:
                            properties = BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK);
                            break;
                        case REAL_TRANSPARENT:
                            properties = BlockBehaviour.Properties.copy(Blocks.END_ROD);
                            break;
                        case REAL_WIRE:
                            properties = BlockBehaviour.Properties.copy(Blocks.GRASS);
                            break;
                        default:
                            throw new RuntimeException("Not implemented!");
                    }

                    internalBlock = new Block(properties);

                    //<editor-fold desc="Inject the block into the Minecraft internal registry">
                    Registry.register(
                            BuiltInRegistries.BLOCK,
                            new ResourceLocation(cached.namespace, cached.key),
                            internalBlock
                    );
                    internalBlock.getStateDefinition().getPossibleStates().forEach(Block.BLOCK_STATE_REGISTRY::add);
                    if(Settings.debug)
                        Bukkit.getLogger().info("Injected block into Minecraft Registry.BLOCK: " + cached.getNamespacedId());
                    //</editor-fold>

                    //<editor-fold desc="Inject the block into the Bukkit lookup data structures to avoid incompatibilities with plugins">
                    try
                    {
                        HashMap<Block, Material> BLOCK_MATERIAL = (HashMap<Block, Material>) field_BLOCK_MATERIAL.get(null);
                        BLOCK_MATERIAL.put(internalBlock, Material.COBBLESTONE); // Dummy Bukkit Material

                        if(Settings.debug)
                            Bukkit.getLogger().info("Injected block into Bukkit lookup: " + cached.getNamespacedId());
                    }
                    catch (IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                    //</editor-fold>
                }
                catch (Throwable e)
                {
                    Bukkit.getLogger().warning("Error registering block '" + cached.getNamespacedId() + "'.");
                    e.printStackTrace();
                }
            }

            if(internalBlock != null)
            {
                registeredBlocks.put(internalBlock.getDescriptionId(), cached);
            }
        });

        Blocks.rebuildCache();
        BuiltInRegistries.BLOCK.freeze();

        if(Bukkit.getPluginManager().getPlugin("Iris") != null)
            IrisHook.inject(customBlocks);

        if(Settings.debug)
            Bukkit.getLogger().info("Finished injecting blocks");
    }

    @Override
    void unfreezeRegistry()
    {
        try
        {
            Field intrusiveHolderCache = Nms.getField(MappedRegistry.class, Map.class, 5);
            intrusiveHolderCache.setAccessible(true);
            intrusiveHolderCache.set(BuiltInRegistries.BLOCK, new IdentityHashMap<Block, Holder.Reference<Block>>());

            Field frozen = Nms.getField(MappedRegistry.class, boolean.class, 0);
            frozen.setAccessible(true);
            frozen.set(BuiltInRegistries.BLOCK, false);
        }
        catch (SecurityException | IllegalArgumentException | IllegalAccessException | NullPointerException e)
        {
            e.printStackTrace();
        }
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
        catch(IllegalArgumentException e) // Block not registered
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
}
