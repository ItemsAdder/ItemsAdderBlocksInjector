package dev.lone.blocksinjector.custom_blocks;

import dev.lone.blocksinjector.custom_blocks.nms.blocksmanager.CustomBlocksInjector;

public class CachedCustomBlockInfo
{
    public final String namespace;
    public final String key;
    public final int itemsAdderId;
    public final Type type;
    public final int spoofedDataId;

    public CachedCustomBlockInfo(String namespacedId, int itemsAdderId, Type type)
    {
        String[] split = namespacedId.split(":");
        this.namespace = split[0];
        this.key = split[1];
        this.itemsAdderId = itemsAdderId;
        this.type = type;
        this.spoofedDataId = CustomBlocksInjector.inst.calculateSpoofedNmsBlockIdFromCachedItemsAdderId(itemsAdderId);
    }

    public String getNamespacedId()
    {
        return namespace + ":" + key;
    }

    public enum Type
    {
        REAL("REAL"),
        REAL_NOTE("REAL_NOTE"),
        REAL_TRANSPARENT("REAL_TRANSPARENT"),
        TILE("TILE"),
        REAL_WIRE("REAL_WIRE"),
        FIRE("FIRE")
        ;

        private final String text;

        Type(final String text)
        {
            this.text = text;
        }

        /**
         * (non-Javadoc)
         * @see java.lang.Enum#toString()
         * @return The readable version of this enum.
         */
        @Override
        public String toString()
        {
            return text;
        }
    }
}
