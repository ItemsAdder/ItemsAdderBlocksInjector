package dev.lone.blocksinjector.custom_blocks;

public class CachedCustomBlockInfo
{
    public final String namespace;
    public final String key;
    public final int id;
    public final Type type;

    public CachedCustomBlockInfo(String namespacedId, int id, Type type)
    {
        String[] split = namespacedId.split(":");
        this.namespace = split[0];
        this.key = split[1];
        this.id = id;
        this.type = type;
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
