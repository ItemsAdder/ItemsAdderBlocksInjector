package dev.lone.blocksinjector.custom_blocks;

public class CachedCustomBlockInfo
{
    public final String namespace;
    public final String key;
    public final int id;

    public CachedCustomBlockInfo(String namespacedId, int id)
    {
        String[] split = namespacedId.split(":");
        this.namespace = split[0];
        this.key = split[1];
        this.id = id;
    }

    public CachedCustomBlockInfo(String namespace, String key, int id)
    {
        this.namespace = namespace;
        this.key = key;
        this.id = id;
    }

    public String getNamespacedId()
    {
        return namespace + ":" + id;
    }
}
