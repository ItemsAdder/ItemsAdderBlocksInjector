package dev.lone.blocksinjector.custom_blocks.nms.blocksmanager;

import com.comphenix.protocol.ProtocolLibrary;
import dev.lone.blocksinjector.Nms;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;

import dev.lone.blocksinjector.custom_blocks.nms.packetlistener.AbstractPacketListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

public abstract class AbstractCustomBlocksManager<B,BS,CP>
{
    Field field_BLOCK_MATERIAL;
    Field BUFFER_FIELD;

    HashMap<B, CachedCustomBlockInfo> registeredBlocks = new HashMap<>();
    HashMap<Integer, B> registeredBlocks_stateIds = new HashMap<>();
    AbstractPacketListener packet;
    Plugin plugin;
    boolean isFirstLoad = true;

    public static AbstractCustomBlocksManager inst;

    public AbstractCustomBlocksManager()
    {
        inst = this;
    }

    public static AbstractCustomBlocksManager initNms(Plugin plugin)
    {
        return Nms.findImplementation(AbstractCustomBlocksManager.class, false, plugin);
    }

    public abstract void load(Plugin plugin, HashMap<CachedCustomBlockInfo, Integer> namespacedBlocks);
    public abstract void loadFromCache(Plugin plugin);
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
}
