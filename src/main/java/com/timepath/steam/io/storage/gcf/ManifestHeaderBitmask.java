package com.timepath.steam.io.storage.gcf;

/**
 * @author TimePath
 */
enum ManifestHeaderBitmask {
    Build_Mode(0x1), Is_Purge_All(0x2), Is_Long_Roll(0x4), Depot_Key(0xFFFFFF00);
    private static final ManifestHeaderBitmask[] flags = ManifestHeaderBitmask.values();
    final int mask;

    ManifestHeaderBitmask(int mask) {
        this.mask = mask;
    }

    static ManifestHeaderBitmask get(int mask) {
        return flags[mask];
    }
}
