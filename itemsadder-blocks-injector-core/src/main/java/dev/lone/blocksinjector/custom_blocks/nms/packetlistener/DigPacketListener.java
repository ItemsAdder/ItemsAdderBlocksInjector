package dev.lone.blocksinjector.custom_blocks.nms.packetlistener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.lone.blocksinjector.Main;
import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.AbstractCustomBlocksManager;
import dev.lone.itemsadder.api.CustomBlock;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * This is a fucking cheat to avoid ItemsAdder breaking get stuck, since the client doesn't stop breaking the block
 * in front of the player if the material of the block itself changes during the animation.
 * This causes ItemsAdder to never receive the START_DESTROY_BLOCK for the custom block (after it gets replaced by the injector).
 *
 * This trick fixes it.
 */
public class DigPacketListener extends AbstractPacketListener
{
    private static boolean registered;

    public DigPacketListener()
    {
        super(Main.inst, ListenerPriority.LOWEST, PacketType.Play.Client.BLOCK_DIG);
    }

    public static void register()
    {
        if(registered)
            return;
        ProtocolLibrary.getProtocolManager().addPacketListener(new DigPacketListener());
        registered = true;
    }

    @Override
    public void onPacketReceiving(PacketEvent e)
    {
        try
        {
            if (e.isCancelled())
                return;

            PacketContainer packet = e.getPacket();
            EnumWrappers.PlayerDigType status = e.getPacket().getPlayerDigTypes().read(0);

            if (status == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK)
            {
                if(packet.getMeta("injectedblock").isPresent())
                    return;

                Player player = e.getPlayer();
                BlockPosition blockPosition = e.getPacket().getBlockPositionModifier().read(0);
                Block block = blockPosition.toLocation(player.getWorld()).getBlock();
                String descriptionId = AbstractCustomBlocksManager.inst.getDescriptionId(block);
                String namespacedId = descriptionId.replace("block.", "").replace(".", ":");
                CustomBlock customBlock = CustomBlock.getInstance(namespacedId);

                if (customBlock != null) // Is a custom injected block
                {
                    if (player.getGameMode() == GameMode.CREATIVE)
                    {
                        Bukkit.getScheduler().runTask(Main.inst, () -> {
                            customBlock.place(block.getLocation());
                        });
                        return;
                    }

                    // Cancel the original packet
                    e.setCancelled(true);

                    // Replce the injected block with the real custom block
                    Bukkit.getScheduler().runTask(Main.inst, () -> {
                        customBlock.place(block.getLocation());
                    });

                    // After some ticks emulate previously cancelled client packet so that ItemsAdder can receive the dig packet
                    // right after the custom block is sent.
                    Bukkit.getScheduler().runTaskLater(Main.inst, () -> {
                        // Add a flag to avoid re-handling the same event again and again causing an infinite loop.
                        packet.setMeta("injectedblock", true);
                        // Resend it to let ItemsAdder finally catch the block break start event and avoid getting stuck in a loop.
                        ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet);
                    }, 3);
                }
            }
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
