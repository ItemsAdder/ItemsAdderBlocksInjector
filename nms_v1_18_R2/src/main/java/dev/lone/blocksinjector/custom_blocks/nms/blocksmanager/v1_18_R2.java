package dev.lone.blocksinjector.custom_blocks.nms.blocksmanager;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.IrisHook;
import dev.lone.blocksinjector.Main;
import dev.lone.blocksinjector.custom_blocks.nms.Nms;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.ItemsAdder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class v1_18_R2 extends AbstractCustomBlocksManager<Block, BlockState, ClientboundLevelChunkPacketData>
{
    public v1_18_R2()
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
    public void load(Plugin plugin, HashMap<CachedCustomBlockInfo, Integer> namespacedBlocks)
    {
        this.plugin = plugin;

        injectBlocks(namespacedBlocks);
    }

    /**
     * Load namespacedIds from IA yml cached ids files
     */
    @Override
    public void loadFromCache()
    {
        String pluginsFolder = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        File storageFolder = new File(new File(pluginsFolder, "ItemsAdder"), "storage");
        if(storageFolder.exists())
        {

            HashMap<CachedCustomBlockInfo, Integer> namespacedBlocks = new HashMap<>();
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_blocks_ids_cache"));
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_blocks_note_ids_cache"));
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_transparent_blocks_ids_cache"));
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_wire_ids_cache"));

            load(plugin, namespacedBlocks);
        }
        else
        {
            throw new RuntimeException("ItemsAdder not installed");
        }
    }

    @Override
    public void registerListener(Plugin plugin)
    {
        this.plugin = plugin;
        if(packet == null)
        {
            packet = new dev.lone.blocksinjector.custom_blocks.nms.packetlistener.v1_18_R2(plugin);
            ProtocolLibrary.getProtocolManager().addPacketListener(packet);
        }
    }

    @Override
    void injectBlocks(HashMap<CachedCustomBlockInfo, Integer> customBlocks)
    {
        Bukkit.getLogger().info("Injecting " + customBlocks.size() + " blocks...");

        unfreezeRegistry();
        customBlocks.forEach((cached, integer) -> {

            Block internalBlock = isBlockAlreadyRegistered(cached);
            if(internalBlock != null)
            {
                if(Main.instance.debug)
                    Bukkit.getLogger().warning("Block '" + internalBlock.getDescriptionId() + "' already registered, skipping.");
            }
            else
            {
                try
                {
                    internalBlock = new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE));

                    //<editor-fold desc="Inject the block into the Minecraft internal registry">
                    Registry.register(
                            Registry.BLOCK,
                            new ResourceLocation(cached.namespace, cached.key),
                            internalBlock
                    );
                    internalBlock.getStateDefinition().getPossibleStates().forEach(Block.BLOCK_STATE_REGISTRY::add);
                    if(Main.instance.debug)
                        Bukkit.getLogger().info("Injected block into Minecraft Registry.BLOCK: " + internalBlock.getDescriptionId());
                    //</editor-fold>

                    //<editor-fold desc="Inject the block into the Bukkit lookup data structures to avoid incompatibilities with plugins">
                    try
                    {

                        HashMap<Block, Material> BLOCK_MATERIAL = (HashMap<Block, Material>) field_BLOCK_MATERIAL.get(null);
                        BLOCK_MATERIAL.put(internalBlock, Material.COBBLESTONE);

                        if(Main.instance.debug)
                            Bukkit.getLogger().info("Injected block into Bukkit lookup: " + internalBlock.getDescriptionId());
                    }
                    catch (IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                    //</editor-fold>
                }
                catch (IllegalStateException e)
                {
                    e.printStackTrace();
                }
            }

            if(internalBlock != null)
            {
                registeredBlocks.put(internalBlock, cached);
                registeredBlocks_stateIds.put(Block.BLOCK_STATE_REGISTRY.getId(internalBlock.defaultBlockState()), internalBlock);
            }

        });

        Blocks.rebuildCache();
        Registry.BLOCK.freeze();

        if(Bukkit.getPluginManager().getPlugin("Iris") != null)
            IrisHook.inject(customBlocks);

        if(Main.instance.debug)
            Bukkit.getLogger().info("Finished injecting blocks");
    }

    @Override
    void unfreezeRegistry()
    {
        try
        {
            Field intrusiveHolderCache = Nms.getField(MappedRegistry.class, Map.class, 5);
            intrusiveHolderCache.setAccessible(true);
            intrusiveHolderCache.set(Registry.BLOCK, new IdentityHashMap<Block, Holder.Reference<Block>>());

            Field frozen = Nms.getField(MappedRegistry.class, boolean.class, 0);
            frozen.setAccessible(true);
            frozen.set(Registry.BLOCK, false);
        }
        catch (SecurityException | IllegalArgumentException | IllegalAccessException | NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void fixBlockInteract(PlayerInteractEvent e)
    {
        Location blockLoc = e.getClickedBlock().getLocation();
        String descriptionId = ((CraftBlock) e.getClickedBlock()).getHandle().getBlockState(new BlockPos(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ())).getBlock().getDescriptionId();
        String namespacedId = descriptionId.replace("block.", "").replace(".", ":");
        CustomBlock customBlock = CustomBlock.getInstance(namespacedId);
        if(customBlock != null) // Is a custom injected block
        {
            e.setCancelled(true);
            e.getPlayer().sendBlockChange(e.getClickedBlock().getLocation(), Material.AIR.createBlockData());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                customBlock.place(e.getClickedBlock().getLocation());
            }, 0L);
        }
    }

    @Override
    public BlockState nmsBlockFromCached(CachedCustomBlockInfo cachedBlock)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) ItemsAdder.getBlockDataByInternalId(cachedBlock.id);
        return bukkitData.getState();
    }

    @Override
    public BlockState nmsBlockStateFromBlockNamespacedId(int id)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) ItemsAdder.getBlockDataByInternalId(id);
        return bukkitData.getState();
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
}
