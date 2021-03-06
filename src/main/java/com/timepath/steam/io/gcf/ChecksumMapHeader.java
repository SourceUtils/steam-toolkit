package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
@SuppressWarnings("FieldCanBeLocal")
class ChecksumMapHeader {

    /**
     * 4 * 4
     */
    static final long SIZE = 16;
    /**
     * Number of checksums.
     */
    private final int checksumCount;
    /**
     * Always 0x00000001.
     */
    private final int dummy0;
    /**
     * Always 0x14893721.
     */
    private final int formatCode;
    /**
     * Number of items.
     */
    private final int itemCount;

    ChecksumMapHeader(@NotNull GCF g) throws IOException {
        @NotNull RandomAccessFileWrapper raf = g.raf;
        formatCode = raf.readULEInt();
        dummy0 = raf.readULEInt();
        itemCount = raf.readULEInt();
        checksumCount = raf.readULEInt();
        g.checksumMapEntries = new ChecksumMapEntry[itemCount];
        raf.skipBytes(g.checksumMapEntries.length * ChecksumMapEntry.SIZE);
        g.checksumEntries = new ChecksumEntry[checksumCount + 0x20];
        raf.skipBytes(g.checksumEntries.length * ChecksumEntry.SIZE);
    }
}
