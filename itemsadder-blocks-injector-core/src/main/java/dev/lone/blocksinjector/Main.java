package dev.lone.blocksinjector;

import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.AbstractCustomBlocksManager;
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
    public static Main instance;

    static
    {
        AbstractCustomBlocksManager.initNms();
        AbstractCustomBlocksManager.inst.loadFromCache();

        instance = (Main) Bukkit.getPluginManager().getPlugin("ItemsAdderBlocksInjector");
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
    private void onPlayerInteractEvent(PlayerInteractEvent e)
    {
        if(e.getHand() == EquipmentSlot.OFF_HAND)
            return;

        if(e.hasBlock())
        {
            AbstractCustomBlocksManager.inst.fixBlockInteract(e);
        }
    }

    //TODO: maybe it's worth adding this to the vanilla command? idk if it's actually useful.
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
//    private void tab(TabCompleteEvent e)
//    {
//        if(e.getBuffer().startsWith("/setblock"))
//        {
//
//        }
//    }
}
