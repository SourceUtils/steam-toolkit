package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import java.io.IOException;

/**
 *
 * @author TimePath
 */
class BlockAllocationTableEntry {

    /**
     * 7 * 4
     */
    static final long SIZE = 28;

    /**
     * Flags for the block entry.
     * 0x200F0000 == Not used
     */
    final int entryType;

    /**
     * The offset for the data contained in this block entry in the file.
     */
    final int fileDataOffset;

    /**
     * The length of the data in this block entry.
     */
    final int fileDataSize;

    /**
     * The index to the first data block of this block entry's data.
     */
    final int firstClusterIndex;

    /**
     * The index of the block entry in the manifest.
     */
    final int manifestIndex;

    /**
     * The next block entry in the series.
     * (N/A if == BlockCount)
     */
    final int nextBlockEntryIndex;

    /**
     * The previous block entry in the series.
     * (N/A if == BlockCount)
     */
    final int previousBlockEntryIndex;

    BlockAllocationTableEntry(RandomAccessFileWrapper raf) throws IOException {
        entryType = raf.readULEInt();
        fileDataOffset = raf.readULEInt();
        fileDataSize = raf.readULEInt();
        firstClusterIndex = raf.readULEInt();
        nextBlockEntryIndex = raf.readULEInt();
        previousBlockEntryIndex = raf.readULEInt();
        manifestIndex = raf.readULEInt();
    }

    @Override
    public String toString() {
        return "type:" + entryType + ", off:" + fileDataOffset + ", size:" + fileDataSize + ", firstidx:"
               + firstClusterIndex + ", nextidx:" + nextBlockEntryIndex + ", previdx:"
               + previousBlockEntryIndex + ", di:" + manifestIndex;
    }

}
