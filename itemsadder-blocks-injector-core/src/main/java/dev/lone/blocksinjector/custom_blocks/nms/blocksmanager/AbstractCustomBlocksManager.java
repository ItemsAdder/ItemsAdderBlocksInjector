package dev.lone.blocksinjector.custom_blocks.nms.blocksmanager;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.custom_blocks.nms.Nms;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.blocksinjector.custom_blocks.nms.packetlistener.AbstractPacketListener;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    Plugin plugin;

    public static AbstractCustomBlocksManager inst;

    public AbstractCustomBlocksManager()
    {
        inst = this;
    }

    public static AbstractCustomBlocksManager initNms()
    {
        return Nms.findImplementation(AbstractCustomBlocksManager.class, false);
    }

    public abstract void load(Plugin plugin, HashMap<CachedCustomBlockInfo, Integer> namespacedBlocks);
    public abstract void loadFromCache();
    public abstract void registerListener(Plugin plugin);
    abstract void injectBlocks(HashMap<CachedCustomBlockInfo, Integer> customBlocks);

    public void unregister()
    {
        if(packet != null)
            ProtocolLibrary.getProtocolManager().removePacketListener(packet);
    }
    abstract void unfreezeRegistry();

    HashMap<CachedCustomBlockInfo, Integer> loadCacheFile(File storageFolder, String cacheFileName)
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
