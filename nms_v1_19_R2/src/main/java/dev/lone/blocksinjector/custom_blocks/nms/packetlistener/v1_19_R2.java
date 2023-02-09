package dev.lone.blocksinjector.custom_blocks.nms.packetlistener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.type.types.version.ChunkSectionType1_18;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.AbstractCustomBlocksManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class v1_19_R2 extends AbstractPacketListener
{
    ChunkSectionType1_18 chunkSectionType = new ChunkSectionType1_18(Block.BLOCK_STATE_REGISTRY.size(), BuiltInRegistries.BIOME_SOURCE.size());

    public v1_19_R2(Plugin plugin)
    {
        super(new PacketAdapter.AdapterParameteters()
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
        // Should I catch not registered blocks (removed from configs) and render them as STONE (for example), to avoid clients crash?
        // Seems the game automatically removes unknown blocks from the region files.

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
                                CachedCustomBlockInfo cachedBlock = AbstractCustomBlocksManager.inst.get(blockData.getBlock());
                                if (cachedBlock != null)
                                {
                                    section.setFlatBlock(
                                            x,
                                            y,
                                            z,
                                            Block.BLOCK_STATE_REGISTRY.getId((BlockState) AbstractCustomBlocksManager.inst.nmsBlockFromCached(cachedBlock))
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

                AbstractCustomBlocksManager.inst.writeByteArrayDataToLevelChunkDataPacket(
                        chunkDataPacket,
                        byteBuf.array()
                );
            }
        }
        else if (e.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE)
        {
            ClientboundBlockUpdatePacket blockUpdate = (ClientboundBlockUpdatePacket) e.getPacket().getHandle();
            if (AbstractCustomBlocksManager.inst.contains(blockUpdate.blockState.getBlock()))
            {
                CachedCustomBlockInfo cachedBlock = AbstractCustomBlocksManager.inst.get(blockUpdate.blockState.getBlock());
                //blockState field
                e.getPacket().getModifier().write(1, AbstractCustomBlocksManager.inst.nmsBlockFromCached(cachedBlock));
            }
        }
        else if (e.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE)
        {
            BlockState[] blockStates = (BlockState[]) e.getPacket().getModifier().read(2);
            // If there isn't any custom block in this packet, let's ignore it
            if (containsNoCustomBlock(blockStates))
                return;

            for (int i = 0, changedBlocksLength = blockStates.length; i < changedBlocksLength; i++)
            {
                BlockState blockState = blockStates[i];
                CachedCustomBlockInfo cachedBlock = AbstractCustomBlocksManager.inst.get(blockState.getBlock());
                if (cachedBlock != null)
                {
                    // And add a wrapped block data!
                    blockStates[i] = (BlockState) AbstractCustomBlocksManager.inst.nmsBlockFromCached(cachedBlock);
                }
                else
                {
                    blockStates[i] = blockState;
                }
            }
        }
    }

    private boolean containsNoCustomBlock(DataPalette blockPalette)
    {
        for (int i = 0; i < blockPalette.size(); i++)
        {
            int paletteId = blockPalette.idByIndex(i);
            if(AbstractCustomBlocksManager.inst.contains(paletteId))
                return false;
        }

        return true;
    }

    private boolean containsNoCustomBlock(BlockState[] changedBlocks)
    {
        for (BlockState changedBlock : changedBlocks)
        {
            if(AbstractCustomBlocksManager.inst.contains(changedBlock.getBlock()))
                return false;
        }
        return true;
    }
}
