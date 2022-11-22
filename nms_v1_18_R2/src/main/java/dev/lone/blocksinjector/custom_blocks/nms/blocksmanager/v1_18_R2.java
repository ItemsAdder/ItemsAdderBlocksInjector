package dev.lone.blocksinjector.custom_blocks.nms.blocksmanager;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.LoneLibs.nbt.nbtapi.NBTItem;
import dev.lone.blocksinjector.IrisHook;
import dev.lone.blocksinjector.Settings;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.blocksinjector.custom_blocks.nms.Nms;
import dev.lone.itemsadder.api.CustomBlock;
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
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
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
                    switch (cached.type)
                    {
                        case REAL:
                        case REAL_NOTE:
                            properties = BlockBehaviour.Properties.copy(Blocks.NOTE_BLOCK);
                            break;
                        case REAL_TRANSPARENT:
                            properties = BlockBehaviour.Properties.copy(Blocks.CHORUS_PLANT);
                            break;
                        case REAL_WIRE:
                            properties = BlockBehaviour.Properties.copy(Blocks.TRIPWIRE);
                            break;
                        default:
                            throw new RuntimeException("Not implemented!");
                    }

                    internalBlock = new Block(properties);

                    //<editor-fold desc="Inject the block into the Minecraft internal registry">
                    Registry.register(
                            Registry.BLOCK,
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
                        BLOCK_MATERIAL.put(internalBlock, Material.COBBLESTONE);

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
                registeredBlocks.put(internalBlock, cached);
                registeredBlocks_stateIds.put(Block.BLOCK_STATE_REGISTRY.getId(internalBlock.defaultBlockState()), internalBlock);
            }

        });

        Blocks.rebuildCache();
        Registry.BLOCK.freeze();

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
            Player player = e.getPlayer();
            if(player.getGameMode() == GameMode.CREATIVE)
            {
                customBlock.place(e.getClickedBlock().getLocation());
            }
            else
            {
                //<editor-fold desc="This is a fucking cheat to make the dig animation stop and avoid getting stuck breaking the block.">
                e.setCancelled(true);
                AtomicReference<ItemStack> prev = new AtomicReference<>();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    prev.set(player.getItemInHand());

                    ItemStack tmp = prev.get().clone();
                    NBTItem n = new NBTItem(tmp);
                    n.setBoolean("sus", true);

                    player.setItemInHand(n.getItem());
                }, 1L);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setItemInHand(prev.get());
                    customBlock.place(e.getClickedBlock().getLocation());
                }, 2L);
                //</editor-fold>
            }
        }
    }

    @Override
    public BlockState nmsBlockFromCached(CachedCustomBlockInfo cachedBlock)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) getItemsAdderBlockDataByInternalId(cachedBlock.id);
        return bukkitData.getState();
    }

    @Override
    public BlockState nmsBlockStateFromBlockNamespacedId(int id)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) getItemsAdderBlockDataByInternalId(id);
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
