package dev.lone.blocksinjector.custom_blocks.nms.blocksmanager;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.Main;
import dev.lone.blocksinjector.custom_blocks.nms.Nms;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.blocksinjector.custom_blocks.nms.packetlistener.AbstractPacketListener;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import dev.lone.blocksinjector.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Not compatible with FAWE.
 * Useful links:
 * - https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/268d8cff49df810c5d8093aa7172606079f3114c/worldedit-bukkit/src/main/java/com/sk89q/worldedit/bukkit/BukkitBlockRegistry.java#L155
 * - https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/345785a25e1eb83e0bb4aa3b8da84e52240772ff/worldedit-bukkit/src/main/java/com/sk89q/worldedit/bukkit/BukkitWorld.java#L136
 */
public abstract class AbstractCustomBlocksManager<B,BS,CP>
{
    Field field_BLOCK_MATERIAL;
    Field BUFFER_FIELD;

    HashMap<B, CachedCustomBlockInfo> registeredBlocks = new HashMap<>();
    HashMap<Integer, B> registeredBlocks_stateIds = new HashMap<>();
    AbstractPacketListener packet;

    public static AbstractCustomBlocksManager inst;

    public AbstractCustomBlocksManager()
    {
        inst = this;
    }

    public static AbstractCustomBlocksManager initNms()
    {
        return Nms.findImplementation(AbstractCustomBlocksManager.class, false);
    }

    public void load(HashMap<CachedCustomBlockInfo, Integer> namespacedBlocks)
    {
        injectBlocks(namespacedBlocks);
    }

    /**
     * Load namespacedIds from IA yml cached ids files
     */
    public void loadFromCache()
    {
        Plugin itemsadder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if(itemsadder == null)
        {
            Main.inst.getLogger().warning("ItemsAdder not installed");
            Bukkit.getServer().shutdown();
            return;
        }

        File storageFolder = new File(itemsadder.getDataFolder(), "storage");
        if(storageFolder.exists())
        {

            HashMap<CachedCustomBlockInfo, Integer> namespacedBlocks = new HashMap<>();
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_blocks_ids_cache", CachedCustomBlockInfo.Type.REAL));
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_blocks_note_ids_cache", CachedCustomBlockInfo.Type.REAL_NOTE));
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_transparent_blocks_ids_cache", CachedCustomBlockInfo.Type.REAL_TRANSPARENT));
            namespacedBlocks.putAll(loadCacheFile(storageFolder, "real_wire_ids_cache", CachedCustomBlockInfo.Type.REAL_WIRE));

            load(namespacedBlocks);
        }
        else
        {
            Main.inst.getLogger().warning("No ItemsAdder/storage folder found.");
            Bukkit.getServer().shutdown();
        }
    }

    public abstract void registerListener();
    abstract void injectBlocks(HashMap<CachedCustomBlockInfo, Integer> customBlocks);

    public void unregister()
    {
        if(packet != null)
            ProtocolLibrary.getProtocolManager().removePacketListener(packet);
    }
    abstract void unfreezeRegistry();

    HashMap<CachedCustomBlockInfo, Integer> loadCacheFile(File storageFolder, String cacheFileName, CachedCustomBlockInfo.Type type)
    {
        HashMap<CachedCustomBlockInfo, Integer> map = new HashMap<>();
        File f = new File(storageFolder, cacheFileName + ".yml");
        if (f.exists())
        {
            FileConfiguration cacheYml = YamlConfiguration.loadConfiguration(f);
            for (String key : cacheYml.getKeys(false))
            {
                int id = cacheYml.getInt(key);
                map.put(new CachedCustomBlockInfo(key, id, type), id);
            }
        }
        return map;
    }

    public abstract void fixBlockInteract(PlayerInteractEvent e);
    public abstract BS nmsBlockFromCached(CachedCustomBlockInfo cachedBlock);
    public abstract BS nmsBlockStateFromBlockNamespacedId(int id);
    @Nullable
    abstract B isBlockAlreadyRegistered(CachedCustomBlockInfo cached);

    public void writeByteArrayDataToLevelChunkDataPacket(CP packet, byte[] data)
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

    public CachedCustomBlockInfo get(B block)
    {
        return registeredBlocks.get(block);
    }

    public boolean contains(B block)
    {
        return registeredBlocks.containsKey(block);
    }

    public boolean contains(int paletteId)
    {
        return registeredBlocks_stateIds.containsKey(paletteId);
    }

    /**
     * Wrap the IA api method in case of future API changes
     */
    BlockData getItemsAdderBlockDataByInternalId(int id)
    {
        return ItemsAdder.Advanced.getBlockDataByInternalId(id);
    }
}
