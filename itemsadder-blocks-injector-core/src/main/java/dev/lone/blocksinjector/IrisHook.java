package dev.lone.blocksinjector;

import com.volmit.iris.util.data.B;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import org.bukkit.Bukkit;

import java.util.HashMap;

public class IrisHook
{
    public static void inject(HashMap<CachedCustomBlockInfo, Integer> customBlocks)
    {
        customBlocks.forEach((cached, integer) -> {
            B.registerCustomBlockData(cached.namespace, cached.key, Bukkit.createBlockData(cached.namespace + ":" + cached.key));
        });
    }
}
