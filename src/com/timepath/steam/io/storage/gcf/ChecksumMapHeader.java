package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;

/**
 * @author TimePath
 */
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

    ChecksumMapHeader(GCF g) throws IOException {
        RandomAccessFileWrapper raf = g.raf; formatCode = raf.readULEInt(); dummy0 = raf.readULEInt();
        itemCount = raf.readULEInt(); checksumCount = raf.readULEInt(); g.checksumMapEntries = new ChecksumMapEntry[itemCount];
        raf.skipBytes(g.checksumMapEntries.length * ChecksumMapEntry.SIZE);
        g.checksumEntries = new ChecksumEntry[checksumCount + 0x20]; raf.skipBytes(g.checksumEntries.length * ChecksumEntry.SIZE);
    }
}
