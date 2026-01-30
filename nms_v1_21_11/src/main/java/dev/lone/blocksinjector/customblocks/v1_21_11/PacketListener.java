package dev.lone.blocksinjector.customblocks.v1_21_11;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.lone.blocksinjector.NMSCustomBlock;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PacketListener extends PacketAdapter {

    public PacketListener(Plugin plugin) {
        super(new AdapterParameteters()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.LOWEST)
                .optionAsync()
                .types(
                        PacketType.Play.Server.MAP_CHUNK,
                        PacketType.Play.Server.BLOCK_CHANGE,
                        PacketType.Play.Server.MULTI_BLOCK_CHANGE,
                        PacketType.Play.Server.WORLD_PARTICLES
                ));
    }

    @Override
    public void onPacketSending(PacketEvent e) {
        switch (e.getPacket().getHandle()) {
            case ClientboundBlockUpdatePacket packet -> {
                Optional<BlockState> newBlockState = getClientBlockState(packet.blockState);
                if (newBlockState.isEmpty()) return;
                e.setPacket(PacketContainer.fromPacket(new ClientboundBlockUpdatePacket(packet.getPos(), newBlockState.get())));
            }
            case ClientboundSectionBlocksUpdatePacket packet -> {
                BlockState[] blockStates = e.getPacket().getSpecificModifier(BlockState[].class).read(0);
                for (int i = 0; i < blockStates.length; i++) {
                    Optional<BlockState> newBlockState = getClientBlockState(blockStates[i]);
                    if (newBlockState.isPresent()) {
                        blockStates[i] = newBlockState.get();
                    }
                }
                e.getPacket().getSpecificModifier(BlockState[].class).write(0, blockStates);
            }
            case ClientboundLevelChunkWithLightPacket packet -> {
                ClientboundLevelChunkPacketData chunkData = packet.getChunkData();
                World world = e.getPlayer().getWorld();
                int worldMinHeight = world.getMinHeight();
                int worldMaxHeight = world.getMaxHeight();
                int worldTrueHeight = (Math.abs(worldMinHeight) + worldMaxHeight);
                int ySectionCount = worldTrueHeight / 16;
                byte[] buffer = processChunkPacket(chunkData, ySectionCount);
                if (buffer != null)
                    e.getPacket().getSpecificModifier(byte[].class).write(0, buffer);
            }
            case ClientboundSetEntityDataPacket(int id, List<SynchedEntityData.DataValue<?>> packedItems) -> {
                ObjectArrayList<SynchedEntityData.DataValue<?>> newItems = new ObjectArrayList<>(packedItems);
                boolean requiresEdit = false;
                for (int i = 0; i < newItems.size(); i++) {
                    SynchedEntityData.DataValue<?> dataValue = newItems.get(i);
                    if (dataValue.value() instanceof BlockState blockState) {
                        Optional<BlockState> newBlockState = getClientBlockState(blockState);
                        if (newBlockState.isPresent()) {
                            requiresEdit = true;
                            newItems.set(i, new SynchedEntityData.DataValue<>(dataValue.id(), EntityDataSerializers.BLOCK_STATE, newBlockState.get()));
                        }
                    }
                }
                if (requiresEdit)
                    e.setPacket(PacketContainer.fromPacket(new ClientboundSetEntityDataPacket(id, newItems)));
            }
            case ClientboundLevelParticlesPacket packet -> {
                if (packet.getParticle() instanceof BlockParticleOption particle) {
                    BlockState blockState = particle.getState();
                    Optional<BlockState> newBlockState = getClientBlockState(blockState);
                    newBlockState.ifPresent(state -> e.setPacket(PacketContainer.fromPacket(
                            new ClientboundLevelParticlesPacket(
                                    new BlockParticleOption(particle.getType(), state),
                                    packet.isOverrideLimiter(),
                                    packet.alwaysShow(),
                                    packet.getX(),
                                    packet.getY(),
                                    packet.getZ(),
                                    packet.getXDist(),
                                    packet.getYDist(),
                                    packet.getZDist(),
                                    packet.getMaxSpeed(),
                                    packet.getCount()
                            )
                    )));
                }
            }
            default -> {
            }
        }
    }

    public static Optional<BlockState> getClientBlockState(@NotNull BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof NMSCustomBlock customBlock) {
            return Optional.of(customBlock.clientBlockState());
        }
        return Optional.empty();
    }

    private static byte @Nullable [] processChunkPacket(@NotNull ClientboundLevelChunkPacketData packet, int sectionCount) {
        FriendlyByteBuf oldBuf = new FriendlyByteBuf(packet.getReadBuffer());
        LevelChunkSection[] sections = new LevelChunkSection[sectionCount];
        boolean requiresEdit = false;

        for (int i = 0; i < sectionCount; i++) {
            //noinspection DataFlowIssue -- It should work fine
            LevelChunkSection section = new LevelChunkSection(PalettedContainerFactory.create(MinecraftServer.getServer().registryAccess()), null, null, 0);
            section.read(oldBuf);

            PalettedContainer<BlockState> container = section.getStates();


            Palette<BlockState> palette = container.data.palette();
            Object[] values = palette.moonrise$getRawPalette(null);

            for (int j = 0; j < values.length; j++) {
                Object obj = values[j];
                if (obj instanceof BlockState state) {
                    Optional<BlockState> clientBlockState = getClientBlockState(state);
                    if (clientBlockState.isPresent()) {
                        values[j] = clientBlockState.get();
                        requiresEdit = true;
                    }
                }
            }

            sections[i] = section;
        }

        if (requiresEdit) {
            FriendlyByteBuf newBuf = new FriendlyByteBuf(Unpooled.buffer());
            for (LevelChunkSection section : sections) {
                //noinspection DataFlowIssue -- actually nullable
                section.write(newBuf, null, 0);
            }
            return newBuf.array();
        }
        return null;
    }

}
