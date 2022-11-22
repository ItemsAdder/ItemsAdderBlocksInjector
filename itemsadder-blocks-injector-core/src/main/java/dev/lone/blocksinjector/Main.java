package dev.lone.blocksinjector;

import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.AbstractCustomBlocksManager;
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

    @Override
    public void onLoad()
    {
        instance = this;

        Settings.init(instance);

        AbstractCustomBlocksManager.initNms();
        AbstractCustomBlocksManager.inst.loadFromCache();

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

    //TODO: Maybe it's worth adding custom blocks to the vanilla command? since they now will be correctly
    // resolved by the internal Minecract code.
    // Idk if it's actually useful.
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
//    private void tab(TabCompleteEvent e)
//    {
//        if(e.getBuffer().startsWith("/setblock"))
//        {
//
//        }
//    }
}
