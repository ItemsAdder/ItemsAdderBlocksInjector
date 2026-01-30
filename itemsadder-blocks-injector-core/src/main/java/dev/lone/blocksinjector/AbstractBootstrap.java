package dev.lone.blocksinjector;

import dev.lone.blocksinjector.customblocks.BlockInjector;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public interface AbstractBootstrap extends PluginBootstrap {

    @Override
    default void bootstrap(@NotNull BootstrapContext bootstrapContext) {
        try {
            blockInjector(ServerBuildInfo.buildInfo().minecraftVersionId()).loadFromCache(bootstrapContext.getLogger());
        } catch (Exception | LinkageError e) {
            bootstrapContext.getLogger().error("Failed to load custom blocks from ItemsAdder cache files.", e);
            System.exit(1);
        }
    }

    BlockInjector blockInjector(String version);

}
