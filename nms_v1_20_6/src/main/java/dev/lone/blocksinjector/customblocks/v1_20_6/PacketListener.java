package dev.lone.blocksinjector.customblocks.v1_20_6;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.chunk.SingleValuePalette;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

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
                        PacketType.Play.Server.ENTITY_METADATA,
                        PacketType.Play.Server.WORLD_PARTICLES
                ));
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(@NotNull PacketEvent e) {
        switch (e.getPacket().getHandle()) {
            case ClientboundBlockUpdatePacket packet -> {
                Optional<BlockState> newBlockState = getClientBlockState(packet.blockState);
                if (newBlockState.isEmpty()) return;
                e.setPacket(PacketContainer.fromPacket(new ClientboundBlockUpdatePacket(packet.getPos(), newBlockState.get())));
            }
            case ClientboundSectionBlocksUpdatePacket packet -> {
                try {
                    BlockState[] blockStates = Reflection.getBlockStates(packet);
                    for (int i = 0; i < blockStates.length; i++) {
                        Optional<BlockState> newBlockState = getClientBlockState(blockStates[i]);
                        if (newBlockState.isPresent()) {
                            blockStates[i] = newBlockState.get();
                        }
                    }
                    Reflection.setBlockStates(packet, blockStates);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            case ClientboundLevelChunkWithLightPacket packet -> {
                ClientboundLevelChunkPacketData chunkData = packet.getChunkData();
                World world = e.getPlayer().getWorld();
                int worldMinHeight = world.getMinHeight();
                int worldMaxHeight = world.getMaxHeight();
                int worldTrueHeight = (Math.abs(worldMinHeight) + worldMaxHeight);
                int ySectionCount = worldTrueHeight / 16;
                try {
                    processChunkPacket(chunkData, ySectionCount);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
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

    private static void processChunkPacket(@NotNull ClientboundLevelChunkPacketData packet, int sectionCount) throws Exception {
        FriendlyByteBuf oldBuf = new FriendlyByteBuf(packet.getReadBuffer());
        LevelChunkSection[] sections = new LevelChunkSection[sectionCount];
        boolean requiresEdit = false;

        for (int i = 0; i < sectionCount; i++) {
            @SuppressWarnings("DataFlowIssue")
            LevelChunkSection section = new LevelChunkSection(MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME), null, null, 0);
            section.read(oldBuf);

            PalettedContainer<BlockState> container = section.getStates();
            Palette<BlockState> palette = Reflection.getPalette(container);
            Object[] values = Reflection.getPaletteValues(palette);

            if (values == null) {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockState blockState = section.getBlockState(x, y, z);
                            Optional<BlockState> clientBlockState = getClientBlockState(blockState);
                            if (clientBlockState.isPresent()) {
                                section.setBlockState(x, y, z, clientBlockState.get());
                                requiresEdit = true;
                            }
                        }
                    }
                }
            } else {
                if (palette instanceof SingleValuePalette<BlockState> singleValuePalette) {
                    Optional<BlockState> blockState = getClientBlockState((BlockState) values[0]);
                    if (blockState.isPresent()) {
                        values[0] = blockState.get();
                        Reflection.setValue(singleValuePalette, blockState.get());
                        requiresEdit = true;
                    }
                } else {
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
            try {
                Reflection.setBuffer(packet, newBuf.array());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
