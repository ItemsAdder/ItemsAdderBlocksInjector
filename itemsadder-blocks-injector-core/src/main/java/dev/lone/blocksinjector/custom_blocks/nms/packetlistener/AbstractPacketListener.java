package dev.lone.blocksinjector.custom_blocks.nms.packetlistener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import org.bukkit.plugin.Plugin;
import dev.lone.blocksinjector.annotations.NotNull;

public abstract class AbstractPacketListener extends PacketAdapter
{
    public AbstractPacketListener(@NotNull PacketAdapter.AdapterParameteters params)
    {
        super(params);
    }

    public AbstractPacketListener(Plugin plugin, PacketType... types)
    {
        super(plugin, types);
    }

    public AbstractPacketListener(Plugin plugin, Iterable<? extends PacketType> types)
    {
        super(plugin, types);
    }

    public AbstractPacketListener(Plugin plugin, ListenerPriority listenerPriority, Iterable<? extends PacketType> types)
    {
        super(plugin, listenerPriority, types);
    }

    public AbstractPacketListener(Plugin plugin, ListenerPriority listenerPriority, Iterable<? extends PacketType> types, ListenerOptions... options)
    {
        super(plugin, listenerPriority, types, options);
    }

    public AbstractPacketListener(Plugin plugin, ListenerPriority listenerPriority, PacketType... types)
    {
        super(plugin, listenerPriority, types);
    }
}
