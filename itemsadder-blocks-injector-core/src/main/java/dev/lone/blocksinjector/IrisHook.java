package dev.lone.blocksinjector;

import dev.lone.blocksinjector.customblocks.BlockInjector;
import dev.lone.blocksinjector.customblocks.CustomBlock;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class IrisHook {

    public static void injectFromBlockInjector() {
        for (CustomBlock customBlock: BlockInjector.BLOCKS) {
            System.out.println("Injecting block " + customBlock.blockId() + " to Iris");
            inject(customBlock);
        }
    }

    public static void inject(@NotNull CustomBlock customBlock) {
        com.volmit.iris.util.data.B.registerCustomBlockData(
                customBlock.blockId().namespace(),
                customBlock.blockId().value(),
                Bukkit.createBlockData(customBlock.blockId().asString())
        );
    }
}
