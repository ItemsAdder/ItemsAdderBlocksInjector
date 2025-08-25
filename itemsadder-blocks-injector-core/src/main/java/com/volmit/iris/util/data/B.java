package com.volmit.iris.util.data;

public interface B {

    /**
     * This is just because Iris doesn't have a public maven repo :(
     * It is just simpler like that.
     * <p>
     * It is excluded from the build so it doesn't cause any issues.
     *
     * @param ignoredNamespace namespace
     * @param ignoredKey key
     * @param ignoredBlockData blockData
     */
    static void registerCustomBlockData(String ignoredNamespace, String ignoredKey, Object ignoredBlockData) {
    }

}
