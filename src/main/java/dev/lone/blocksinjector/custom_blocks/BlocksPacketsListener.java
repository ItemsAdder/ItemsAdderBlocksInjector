package dev.lone.blocksinjector.custom_blocks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.type.types.version.ChunkSectionType1_18;
import dev.lone.itemsadder.Core.ItemTypes.CustomBlockItem;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlocksPacketsListener extends PacketAdapter
{
    //TODO: don't use Viaversion, implement this without ChunkSectionType1_18 class!!!
    ChunkSectionType1_18 chunkSectionType = new ChunkSectionType1_18(Block.BLOCK_STATE_REGISTRY.size(), Registry.BIOME_SOURCE.size());

    public BlocksPacketsListener(Plugin plugin)
    {
        super(new AdapterParameteters()
                      .plugin(plugin)
                      .listenerPriority(ListenerPriority.LOWEST)
                      .optionAsync()
                      .types(
                              PacketType.Play.Server.MAP_CHUNK,
                              PacketType.Play.Server.BLOCK_CHANGE,
                              PacketType.Play.Server.MULTI_BLOCK_CHANGE
                      ));
    }

    // https://gist.github.com/MrPowerGamerBR/51bf5beb6466d557da2191ed8a3fe0df
    @Override
    public void onPacketSending(PacketEvent e)
    {
        PacketContainer packet = e.getPacket();

        if (e.getPacketType() == PacketType.Play.Server.MAP_CHUNK)
        {
            ClientboundLevelChunkWithLightPacket handle = (ClientboundLevelChunkWithLightPacket) packet.getHandle();
            ClientboundLevelChunkPacketData chunkDataPacket = handle.getChunkData();

            World world = e.getPlayer().getWorld();
            int worldMinHeight = world.getMinHeight();
            int worldMaxHeight = world.getMaxHeight();
            int worldTrueHeight = (Math.abs(worldMinHeight) + worldMaxHeight);
            int ySectionCount = worldTrueHeight / 16;

            // And then we read the byte array from there!
            FriendlyByteBuf readBuffer = chunkDataPacket.getReadBuffer();
            byte[] byteArray = readBuffer.array();
            ByteBuf buf = Unpooled.copiedBuffer(byteArray);

            List<ChunkSection> sections = new ArrayList<>();
            for (int i = 0; i < ySectionCount; i++)
            {
                try
                {
                    sections.add(chunkSectionType.read(buf));
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

            AtomicBoolean requiresEdits = new AtomicBoolean();
            sections.forEach(section -> {
                @Nullable DataPalette blockPalette = section.palette(PaletteType.BLOCKS);
                if (blockPalette == null) // Does not have any palette...
                    return;

                if(containsNoCustomBlock(blockPalette))
                    return;

                boolean hasCustomBlocks = false;

                //TODO: Use this instead, probably more optimized.
                // Basically only change the palette entry instead of iterating the whole-ass 16x16x16 chunk section.
//                for (int i = 0; i < section.getPaletteSize(); i++)
//                {
//                    section.replacePaletteEntry(i, );
//                }

                for (int y = 0; y < 16; y++)
                {
                    for (int x = 0; x < 16; x++)
                    {
                        for (int z = 0; z < 16; z++)
                        {
                            BlockState blockData = Block.BLOCK_STATE_REGISTRY.byId(section.getFlatBlock(x, y, z));
                            if (blockData != null)
                            {
                                CustomBlockItem customBlockItem = CustomBlocksManager.inst.registeredBlocks.get(blockData.getBlock());
                                if (customBlockItem != null)
                                {
                                    section.setFlatBlock(
                                            x,
                                            y,
                                            z,
                                            Block.BLOCK_STATE_REGISTRY.getId(CustomBlocksManager.nmsBlockStateFromCustomItem(customBlockItem))
                                    );
                                    hasCustomBlocks = true;
                                }
                            }
                        }
                    }
                }

                if (!requiresEdits.get())
                    requiresEdits.set(hasCustomBlocks);
            });

            //TODO: why is sections iterated two times? WTF
            if (requiresEdits.get())
            {
                // println("Requires edit, so we are going to clear the read buffer")
                // Only rewrite the packet if we really need to edit the packet
                ByteBuf byteBuf = Unpooled.buffer();
                sections.forEach(chunkSection -> {
                    if (chunkSection == null)
                        return;

                    try
                    {
                        chunkSectionType.write(byteBuf, chunkSection);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                });

                CustomBlocksManager.writeByteArrayDataToLevelChunkDataPacket(
                        chunkDataPacket,
                        byteBuf.array()
                );
            }
        }
        else if (e.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE)
        {
            ClientboundBlockUpdatePacket blockUpdate = (ClientboundBlockUpdatePacket) e.getPacket().getHandle();
            if (CustomBlocksManager.inst.registeredBlocks.containsKey(blockUpdate.blockState.getBlock()))
            {
                CustomBlockItem customBlockItem = CustomBlocksManager.inst.registeredBlocks.get(blockUpdate.blockState.getBlock());
                //blockState field
                e.getPacket().getModifier().write(1, CustomBlocksManager.nmsBlockStateFromCustomItem(customBlockItem));
            }
        }
        else if (e.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE)
        {
            BlockState[] blockStates = (BlockState[]) e.getPacket().getModifier().read(2);
            // If there isn't any custom block in this packet, let's ignore it
            if (containsNoCustomBlock(blockStates))
                return;

            // If there's any note blocks in this packet...
            for (int i = 0, changedBlocksLength = blockStates.length; i < changedBlocksLength; i++)
            {
                BlockState blockState = blockStates[i];
                CustomBlockItem customBlockItem = CustomBlocksManager.inst.registeredBlocks.get(blockState.getBlock());
                if (customBlockItem != null)
                {
                    // And add a wrapped block data!
                    blockStates[i] = CustomBlocksManager.nmsBlockStateFromCustomItem(customBlockItem);
                }
                else
                {
                    blockStates[i] = blockState;
                }
            }

            // And write the new array to the packet
            //e.getPacket().getBlockDataArrays().write(0, blockStates);
        }
    }

    private boolean containsNoCustomBlock(DataPalette blockPalette) // TODO: don't use Viaversion "DataPalette"
    {
        for (int i = 0; i < blockPalette.size(); i++)
        {
            int paletteId = blockPalette.idByIndex(i);
            if(CustomBlocksManager.inst.registeredBlocks_stateIds.containsKey(paletteId))
                return false;
        }

        return true;
    }

    private boolean containsNoCustomBlock(BlockState[] changedBlocks)
    {
        for (BlockState changedBlock : changedBlocks)
        {
            if(CustomBlocksManager.inst.registeredBlocks.containsKey(changedBlock.getBlock()))
                return false;
        }
        return true;
    }
}
