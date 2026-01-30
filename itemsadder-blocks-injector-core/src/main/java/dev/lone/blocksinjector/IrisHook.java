package dev.lone.blocksinjector;

import dev.lone.blocksinjector.customblocks.CustomBlock;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class IrisHook {
    public static void inject(@NotNull CustomBlock customBlock) {
        com.volmit.iris.util.data.B.registerCustomBlockData(
                customBlock.blockId().namespace(),
                customBlock.blockId().value(),
                Bukkit.createBlockData(customBlock.blockId().asString())
        );
    }
}
