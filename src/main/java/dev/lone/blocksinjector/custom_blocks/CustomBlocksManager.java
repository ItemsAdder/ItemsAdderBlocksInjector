package dev.lone.blocksinjector.custom_blocks;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.Nms;
import dev.lone.itemsadder.Core.Core;
import dev.lone.itemsadder.Core.ItemTypes.CustomBlockItem;
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

    public HashMap<Block, CustomBlockItem> registeredBlocks = new HashMap<>();
    public HashMap<Integer, Block> registeredBlocks_stateIds = new HashMap<>();
    BlocksPacketsListener packet;
    Plugin plugin;
    boolean isFirstLoad = true;

    public static CustomBlocksManager inst;

    public CustomBlocksManager()
    {
        inst = this;
    }

    public void loadFromIaApi(Plugin plugin)
    {
        load(plugin, new HashSet<>(ItemsAdder.getNamespacedBlocksNamesInConfig()));
    }

    public void load(Plugin plugin, Set<String> namespacedBlocks)
    {
        this.plugin = plugin;

        //<editor-fold desc="Shitty">
        List<CustomBlockItem> customBlocks = new ArrayList<>();
        for (String s : namespacedBlocks)
        {
            customBlocks.add(Core.inst().getOriginalCustomBlockItem(s));
        }
        //</editor-fold>
        injectBlocks(customBlocks);

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

            Set<String> namespacedBlocks = new HashSet<>();
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_blocks_ids_cache"));
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_blocks_note_ids_cache"));
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_transparent_blocks_ids_cache"));
            namespacedBlocks.addAll(loadCacheFile(storageFolder, "real_wire_ids_cache"));

            load(plugin, namespacedBlocks);
        }
        else
        {
            throw new RuntimeException("ItemsAdder not installed");
        }
    }

    private Set<String> loadCacheFile(File storageFolder, String cacheFileName)
    {
        File f = new File(storageFolder, cacheFileName + ".yml");
        if(!f.exists())
            return new LinkedHashSet<>();

        FileConfiguration cacheYml = YamlConfiguration.loadConfiguration(f);
        return cacheYml.getKeys(false);
    }

    private void injectBlocks(List<CustomBlockItem> customBlocks)
    {
        unfreezeRegistry();
        for (CustomBlockItem customBlock : customBlocks)
        {
            Block internalBlock = null;
            try
            {
                internalBlock = new Block(BlockBehaviour.Properties.copy(CUSTOM_BLOCK_FAKE_BASE_BLOCK));

                //<editor-fold desc="Inject the block into the Minecraft internal registry">
                Registry.register(
                        Registry.BLOCK,
                        new ResourceLocation(customBlock.namespace, customBlock.id),
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
                    if (state.getBlock().getDescriptionId().equals("block." + customBlock.namespace + "." + customBlock.id))
                    {
                        internalBlock = state.getBlock();
                        plugin.getLogger().warning("Block '" + internalBlock.getDescriptionId() + "' already registered, skipping.");
                        break;
                    }
                }

            }

            if(internalBlock != null)
            {
                registeredBlocks.put(internalBlock, customBlock);
                registeredBlocks_stateIds.put(Block.BLOCK_STATE_REGISTRY.getId(internalBlock.defaultBlockState()), internalBlock);
            }
        }

        Blocks.rebuildCache();
        Registry.BLOCK.freeze();

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
            if (intrusiveHolderCache != null)
            {
                intrusiveHolderCache.setAccessible(true);
                intrusiveHolderCache.set(Registry.BLOCK, new IdentityHashMap<Block, Holder.Reference<Block>>());
            }

            Field frozen = Nms.getField(MappedRegistry.class, boolean.class, 0);
            if (frozen != null)
            {
                frozen.setAccessible(true);
                frozen.set(Registry.BLOCK, false);
            }
        }
        catch (SecurityException | IllegalArgumentException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    public static BlockState nmsBlockStateFromCustomItem(CustomBlockItem customBlockItem)
    {
        //TODO: handle spawner blocks, I don't care, I won't support them. They are TILE entities and harder to support.
        CraftBlockData bukkitData = (CraftBlockData) Bukkit.createBlockData(customBlockItem.placedBlockModel.getBlockData().getAsString(true));
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
