package dev.lone.blocksinjector.custom_blocks;

import com.comphenix.protocol.ProtocolLibrary;
import com.volmit.iris.util.data.B;
import dev.lone.blocksinjector.Nms;
import dev.lone.itemsadder.api.ItemsAdder;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

/**
 *
 * Downsides:
 * - the blocks can only correctly loaded if the world is not the "default" world, because the plugin must reload the worlds
 *   when ItemAdder registers its blocks, or the game can't know wtf are these blocks and just removes them from the region file.
 *
 */
public class CustomBlocksManager
{
    public static final Block CUSTOM_BLOCK_FAKE_BASE_BLOCK = Blocks.COBBLESTONE;
    public static final Material CUSTOM_BLOCK_FAKE_BASE_MAT = Material.COBBLESTONE;

    static Field field_BLOCK_MATERIAL;
    static Field field_MATERIAL_BLOCK;
    static Field BUFFER_FIELD;

    static
    {
        try
        {
            field_BLOCK_MATERIAL = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            field_BLOCK_MATERIAL.setAccessible(true);

            field_MATERIAL_BLOCK = CraftMagicNumbers.class.getDeclaredField("MATERIAL_BLOCK");
            field_MATERIAL_BLOCK.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        BUFFER_FIELD = ClientboundLevelChunkPacketData.class.getDeclaredFields()[2];
        BUFFER_FIELD.setAccessible(true);
    }

    public HashMap<Block, CachedCustomBlockInfo> registeredBlocks = new HashMap<>();
    public HashMap<Integer, Block> registeredBlocks_stateIds = new HashMap<>();
    BlocksPacketsListener packet;
    Plugin plugin;
    boolean isFirstLoad = true;

    public static CustomBlocksManager inst;

    public CustomBlocksManager()
    {
        inst = this;
    }

    public void load(Plugin plugin, HashMap<CachedCustomBlockInfo, Integer> namespacedBlocks)
    {
        this.plugin = plugin;

        injectBlocks(namespacedBlocks);

        if(packet == null)
        {
            packet = new BlocksPacketsListener(plugin);
            ProtocolLibrary.getProtocolManager().addPacketListener(packet);
        }

        if(!isFirstLoad)
        {
            for (World world : Bukkit.getServer().getWorlds())
            {
                boolean unloaded = Bukkit.unloadWorld(world, false);
                if (unloaded)
                    Bukkit.getServer().createWorld(new WorldCreator(world.getName()));
            }
        }

        isFirstLoad = false;
    }

    /**
     * Load namespacedIds from IA yml cached ids files
     * @param plugin this plugin
     */
    public void loadFromCache(Plugin plugin)
    {
        File storageFolder = new File(new File(plugin.getDataFolder().getParent(), "ItemsAdder"), "storage");
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

    private HashMap<CachedCustomBlockInfo, Integer> loadCacheFile(File storageFolder, String cacheFileName)
    {
        HashMap<CachedCustomBlockInfo, Integer> map = new HashMap<>();
        File f = new File(storageFolder, cacheFileName + ".yml");
        if (f.exists())
        {
            FileConfiguration cacheYml = YamlConfiguration.loadConfiguration(f);
            for (String key : cacheYml.getKeys(false))
            {
                int id = cacheYml.getInt(key);
                map.put(new CachedCustomBlockInfo(key, id), id);
            }
        }
        return map;
    }

    private void injectBlocks(HashMap<CachedCustomBlockInfo, Integer> customBlocks)
    {
        plugin.getLogger().info("Injecting " + customBlocks.size() + " blocks...");

        unfreezeRegistry();
        customBlocks.forEach((cached, integer) -> {

            Block internalBlock = null;
            try
            {
                internalBlock = new Block(BlockBehaviour.Properties.copy(CUSTOM_BLOCK_FAKE_BASE_BLOCK));

                //<editor-fold desc="Inject the block into the Minecraft internal registry">
                Registry.register(
                        Registry.BLOCK,
                        new ResourceLocation(cached.namespace, cached.key),
                        internalBlock
                );
                internalBlock.getStateDefinition().getPossibleStates().forEach(Block.BLOCK_STATE_REGISTRY::add);
                plugin.getLogger().info("Injected block into Minecraft Registry.BLOCK: " + internalBlock.getDescriptionId());
                //</editor-fold>


                //<editor-fold desc="Inject the block into the Bukkit lookup data structures to avoid incompatibilities with plugins">
                try
                {

                    HashMap<Block, Material> BLOCK_MATERIAL = (HashMap<Block, Material>) field_BLOCK_MATERIAL.get(null);
                    BLOCK_MATERIAL.put(internalBlock, CUSTOM_BLOCK_FAKE_BASE_MAT);

                    HashMap<Material, Block> MATERIAL_BLOCK = (HashMap<Material, Block>) field_MATERIAL_BLOCK.get(null);
                    MATERIAL_BLOCK.put(CUSTOM_BLOCK_FAKE_BASE_MAT, internalBlock);

                    plugin.getLogger().info("Injected block into Bukkit lookup: " + internalBlock.getDescriptionId());
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
                //</editor-fold>
            }
            catch (IllegalStateException e)
            {
                // TODO: recode this shit
                for (BlockState state : Block.BLOCK_STATE_REGISTRY)
                {
                    if (state.getBlock().getDescriptionId().equals("block." + cached.namespace + "." + cached.key))
                    {
                        internalBlock = state.getBlock();
                        plugin.getLogger().warning("Block '" + internalBlock.getDescriptionId() + "' already registered, skipping.");
                        break;
                    }
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

        //TODO: skip if Iris is not installed.
        // Inject into Iris
        customBlocks.forEach((cached, integer) -> {
            B.registerCustomBlockData(cached.namespace, cached.key, Bukkit.createBlockData(cached.namespace + ":" + cached.key));
        });

        plugin.getLogger().info("Finished injecting blocks");
    }

    public void unregister()
    {
        if(packet != null)
            ProtocolLibrary.getProtocolManager().removePacketListener(packet);
    }

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

    public static BlockState nmsBlockFromCached(CachedCustomBlockInfo cachedBlock)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) ItemsAdder.getBlockDataByInternalId(cachedBlock.id);
        return bukkitData.getState();
    }

    public static BlockState nmsBlockStateFromBlockNamespacedId(int id)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) ItemsAdder.getBlockDataByInternalId(id);
        return bukkitData.getState();
    }

    public static void writeByteArrayDataToLevelChunkDataPacket(ClientboundLevelChunkPacketData packet, byte[] data)
    {
        try
        {
            BUFFER_FIELD.set(packet, data);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}
