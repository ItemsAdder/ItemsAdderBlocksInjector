package dev.lone.blocksinjector.customblocks.v1_21_6;

import dev.lone.blocksinjector.customblocks.CustomBlock;
import dev.lone.itemsadder.api.ItemsAdder;
import net.kyori.adventure.key.Key;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public class NMSCustomBlock extends Block implements CustomBlock {

    private BlockState clientBlockState;
    private final int itemsAdderId;
    private final Key blockId;

    public NMSCustomBlock(Properties properties, int itemsAdderId, Key blockId) {
        super(properties);
        this.itemsAdderId = itemsAdderId;
        this.blockId = blockId;
    }

    public BlockState clientBlockState() {
        if (clientBlockState == null) {
            CraftBlockData blockData = (CraftBlockData) ItemsAdder.Advanced.getBlockDataByInternalId(itemsAdderId);
            if (blockData == null) throw new RuntimeException("Failed to get block data for " + itemsAdderId);
            clientBlockState = blockData.getState();
        }
        return clientBlockState;
    }

    @Override
    public Key blockId() {
        return blockId;
    }
}
