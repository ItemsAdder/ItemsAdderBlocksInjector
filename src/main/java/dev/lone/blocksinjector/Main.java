package dev.lone.blocksinjector;

import dev.lone.blocksinjector.custom_blocks.CustomBlocksManager;
import dev.lone.itemsadder.Core.Core;
import dev.lone.itemsadder.Core.ItemTypes.CustomBlockItem;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import net.minecraft.core.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener
{
    CustomBlocksManager blocksManager;

    @Override
    public void onLoad()
    {
        blocksManager = new CustomBlocksManager();
        blocksManager.loadFromCache(this);
    }

    @Override
    public void onEnable()
    {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
        blocksManager.unregister();
    }


    @EventHandler
    public void onItemsAdderLoadData(ItemsAdderLoadDataEvent e)
    {
        if(e.getCause() == ItemsAdderLoadDataEvent.Cause.FIRST_LOAD)
            return;
        blocksManager.loadFromCache(this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void place(CustomBlockPlaceEvent e)
    {
        e.setCancelled(true);
        e.getItemInHand().setAmount(e.getItemInHand().getAmount() - 1);

        e.getBlock().setBlockData(Bukkit.createBlockData(e.getNamespacedID()), false);

        //world.setBlock(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), CustomBlocksManager.SUSSY.defaultBlockState(), 2);
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerInteractEvent(PlayerInteractEvent e)
    {
        if(e.getHand() == EquipmentSlot.OFF_HAND)
            return;

        if(e.hasBlock())
        {
            Location blockLoc = e.getClickedBlock().getLocation();
            String descriptionId = ((CraftBlock) e.getClickedBlock()).getHandle().getBlockState(new BlockPos(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ())).getBlock().getDescriptionId();
            e.getPlayer().sendMessage(descriptionId);
            String namespacedId = descriptionId.replace("block.", "").replace(".", ":");
            CustomBlockItem originalCustomBlockItem = Core.inst().getOriginalCustomBlockItem(namespacedId);
            if(originalCustomBlockItem != null) // Is a custom injected block
            {
                e.setCancelled(true);
                e.getClickedBlock().setType(Material.AIR, false);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    originalCustomBlockItem.place(e.getClickedBlock());
                    e.getPlayer().sendMessage("Fixed injected block!");
                }, 4L);
            }
        }
    }
}
