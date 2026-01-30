package dev.lone.blocksinjector;

import dev.lone.blocksinjector.customblocks.BlockInjector;

public class Bootstrap implements AbstractBootstrap {

    @Override
    public BlockInjector blockInjector(String version) {
        return Nms.getBlockInjector(version);
    }
}
