package dev.lone.blocksinjector.custom_blocks.nms.packetlistener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.type.types.chunk.ChunkSectionType1_18;
import dev.lone.blocksinjector.custom_blocks.CachedCustomBlockInfo;
import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.CustomBlocksInjector;
import dev.lone.itemsadder.api.CustomBlock;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inspired by https://gist.github.com/MrPowerGamerBR/51bf5beb6466d557da2191ed8a3fe0df
 */
public class v1_20_R3 extends AbstractPacketListener
{
    ChunkSectionType1_18 viaSectionIo = new ChunkSectionType1_18(Block.BLOCK_STATE_REGISTRY.size(), BuiltInRegistries.BIOME_SOURCE.size());

    public v1_20_R3(Plugin plugin)
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

    @Override
    public void onPacketSending(PacketEvent e)
    {
        try
        {
            PacketContainer packet = e.getPacket();
            // Should I catch not registered blocks (removed from configs) and render them as STONE (for example), to avoid clients crash?
            // Seems the game automatically removes unknown blocks from the region files on world.

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
                byte[] byteArray = chunkDataPacket.getReadBuffer().array();
                ByteBuf buf = Unpooled.copiedBuffer(byteArray);

                List<ChunkSection> viaSections = new ArrayList<>();
                for (int i = 0; i < ySectionCount; i++)
                {
                    try
                    {
                        viaSections.add(viaSectionIo.read(buf));
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }

                AtomicBoolean requiresEdits = new AtomicBoolean();
                // No need to iterate all blocks of the sections
                viaSections.forEach(viaSection -> {
                    DataPalette viaPalette = viaSection.palette(PaletteType.BLOCKS);
                    if (viaPalette == null)
                        return;

                    for (int i = 0; i < viaPalette.size(); i++)
                    {
                        BlockState realBlockData = Block.BLOCK_STATE_REGISTRY.byId(viaPalette.idByIndex(i));
                        //noinspection ConstantConditions null check (should not be null in normal cases)
                        CachedCustomBlockInfo cachedBlock = CustomBlocksInjector.inst.get(realBlockData.getBlock().getDescriptionId());
                        // If it's a custom block
                        if (cachedBlock != null)
                        {
                            viaPalette.setIdByIndex(i, cachedBlock.spoofedDataId);
                            requiresEdits.set(true);
                        }
                    }
                });

                // Write the buffer only if something changed.
                if (requiresEdits.get())
                {
                    // println("Requires edit, so we are going to clear the read buffer")
                    // Only rewrite the packet if we really need to edit the packet
                    ByteBuf byteBuf = Unpooled.buffer();
                    viaSections.forEach(chunkSection -> {
                        if (chunkSection == null)
                            return;

                        try
                        {
                            viaSectionIo.write(byteBuf, chunkSection);
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    });

                    //noinspection unchecked
                    CustomBlocksInjector.inst.writeByteArrayDataToLevelChunkDataPacket(
                            chunkDataPacket,
                            byteBuf.array()
                    );
                }
            }
            else if (e.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE)
            {
                ClientboundBlockUpdatePacket blockUpdate = (ClientboundBlockUpdatePacket) e.getPacket().getHandle();
                if (CustomBlocksInjector.inst.contains(blockUpdate.blockState.getBlock().getDescriptionId()))
                {
                    CachedCustomBlockInfo cachedBlock = CustomBlocksInjector.inst.get(blockUpdate.blockState.getBlock().getDescriptionId());
                    //blockState field
                    e.getPacket().getModifier().write(1, CustomBlocksInjector.inst.calculateSpoofedNmsBlockFromItemsAdderCachedId(cachedBlock));
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
                    CachedCustomBlockInfo cachedBlock = CustomBlocksInjector.inst.get(blockState.getBlock().getDescriptionId());
                    if (cachedBlock != null)
                    {
                        // And add a wrapped block data!
                        blockStates[i] = (BlockState) CustomBlocksInjector.inst.calculateSpoofedNmsBlockFromItemsAdderCachedId(cachedBlock);
                    }
                    else
                    {
                        blockStates[i] = blockState;
                    }
                }
            }
            else if (e.getPacketType() == PacketType.Play.Server.WORLD_PARTICLES)
            {
                ClientboundLevelParticlesPacket particlesPacket = (ClientboundLevelParticlesPacket) e.getPacket().getHandle();
                if (particlesPacket.getParticle() instanceof BlockParticleOption)
                {
                    BlockParticleOption blockParticleOption = (BlockParticleOption) particlesPacket.getParticle();
                    if (dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.v1_20_R3.inst.REGISTRY.contains(blockParticleOption.getState().getBlock()))
                    {
                        e.setCancelled(true);

                        // block.namespace.id
                        String descriptionId = blockParticleOption.getState().getBlock().getDescriptionId();
                        String[] split = descriptionId.split("\\.");
                        NamespacedKey key = new NamespacedKey(split[1], split[2]);
                        BlockData itemsaAdderBlockData = CustomBlock.getBaseBlockData(key.toString());

                        BlockParticleOption newBlockstateOption = new BlockParticleOption(blockParticleOption.getType(), ((CraftBlockData) itemsaAdderBlockData).getState());
                        ClientboundLevelParticlesPacket newPacket = new ClientboundLevelParticlesPacket(
                                newBlockstateOption,
                                particlesPacket.isOverrideLimiter(),
                                particlesPacket.getX(),
                                particlesPacket.getY(),
                                particlesPacket.getZ(),
                                particlesPacket.getXDist(),
                                particlesPacket.getYDist(),
                                particlesPacket.getZDist(),
                                particlesPacket.getMaxSpeed(),
                                particlesPacket.getCount()
                        );
                        ((CraftPlayer) e.getPlayer()).getHandle().connection.send(newPacket);
                    }
                }
            }
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }

    private boolean containsNoCustomBlock(BlockState[] changedBlocks)
    {
        for (BlockState changedBlock : changedBlocks)
        {
            if (CustomBlocksInjector.inst.contains(changedBlock.getBlock().getDescriptionId()))
                return false;
        }
        return true;
    }
}
