package com.timepath.steam.io.gcf;

import com.timepath.DataUtils;
import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
class BlockAllocationTableHeader {

    /**
     * 8 * 4
     */
    static final long SIZE = 32;
    final long pos;
    /**
     * Number of data blocks.
     */
    private final int blockCount;
    /**
     * Number of data blocks that point to data.
     */
    private final int blocksUsed;
    /**
     * Header checksum.
     * The checksum is simply the sum total of all the preceeding DWORDs in the header
     */
    private final int checksum;
    /**
     *
     */
    private final int dummy0;
    /**
     *
     */
    private final int dummy1;
    /**
     *
     */
    private final int dummy2;
    /**
     *
     */
    private final int dummy3;
    /**
     *
     */
    private final int lastBlockUsed;

    BlockAllocationTableHeader(@NotNull GCF g) throws IOException {
        @NotNull RandomAccessFileWrapper raf = g.raf;
        pos = raf.getFilePointer();
        blockCount = raf.readULEInt();
        blocksUsed = raf.readULEInt();
        lastBlockUsed = raf.readULEInt();
        dummy0 = raf.readULEInt();
        dummy1 = raf.readULEInt();
        dummy2 = raf.readULEInt();
        dummy3 = raf.readULEInt();
        checksum = raf.readULEInt();
        g.blocks = new BlockAllocationTableEntry[blockCount];
        raf.skipBytes(g.blocks.length * BlockAllocationTableEntry.SIZE);
    }

    @NotNull
    @Override
    public String toString() {
        int checked = check();
        @NotNull String checkState = (checksum == checked) ? "OK" : (checksum + " vs " + checked);
        return "blockCount:" + blockCount + ", blocksUsed:" + blocksUsed + ", check:" + checkState;
    }

    int check() {
        int checked = 0;
        checked += DataUtils.updateChecksumAdd(blockCount);
        checked += DataUtils.updateChecksumAdd(blocksUsed);
        checked += DataUtils.updateChecksumAdd(lastBlockUsed);
        checked += DataUtils.updateChecksumAdd(dummy0);
        checked += DataUtils.updateChecksumAdd(dummy1);
        checked += DataUtils.updateChecksumAdd(dummy2);
        checked += DataUtils.updateChecksumAdd(dummy3);
        return checked;
    }
}
