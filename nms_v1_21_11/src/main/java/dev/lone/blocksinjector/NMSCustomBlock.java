package dev.lone.blocksinjector;

import dev.lone.blocksinjector.customblocks.CustomBlock;
import net.kyori.adventure.key.Key;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NMSCustomBlock extends Block implements CustomBlock {

    private final BlockState clientBlockState;
    private final Key blockId;

    public NMSCustomBlock(Properties properties, BlockState clientBlockState, Key blockId) {
        super(properties);
        this.clientBlockState = clientBlockState;
        this.blockId = blockId;
    }

    public BlockState clientBlockState() {
        return clientBlockState;
    }

    @Override
    public Key blockId() {
        return blockId;
    }
}
