package dev.lone.blocksinjector;

import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.AbstractCustomBlocksManager;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener
{
    // Important, to load before all plugins, for example FastAsyncWorldEdit is reading the Blocks registry and can't
    // hook into the custom blocks because they don't exist in the onLoad/onEnable event.
    static
    {
        AbstractCustomBlocksManager.initNms();
        AbstractCustomBlocksManager.inst.loadFromCache();

        if(Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null)
        {
            //BlockTypesCache.states // TODO: I might need to reset this because it seems that FAWE is caching blocks and bugs out
        }

    }

    @Override
    public void onLoad()
    {
        AbstractCustomBlocksManager.inst.registerListener(this);
    }

    @Override
    public void onEnable()
    {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
        AbstractCustomBlocksManager.inst.unregister();
    }

    @EventHandler
    public void onItemsAdderLoadData(ItemsAdderLoadDataEvent e)
    {
        if(e.getCause() == ItemsAdderLoadDataEvent.Cause.FIRST_LOAD)
            return;
        AbstractCustomBlocksManager.inst.loadFromCache();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void place(CustomBlockPlaceEvent e)
    {
        e.setCancelled(true);
        e.getItemInHand().setAmount(e.getItemInHand().getAmount() - 1);

        e.getBlock().setBlockData(Bukkit.createBlockData(e.getNamespacedID()), false);
    }

    //TODO: refactor this shit
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerInteractEvent(PlayerInteractEvent e)
    {
        if(e.getHand() == EquipmentSlot.OFF_HAND)
            return;

        if(e.hasBlock())
        {
            AbstractCustomBlocksManager.inst.fixBlockInteract(e);
        }
    }

    //TODO
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
//    private void tab(TabCompleteEvent e)
//    {
//        if(e.getBuffer().startsWith("/setblock"))
//        {
//
//        }
//    }
}
