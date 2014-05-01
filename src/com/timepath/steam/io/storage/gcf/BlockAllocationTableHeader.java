package com.timepath.steam.io.storage.gcf;

import com.timepath.DataUtils;
import com.timepath.io.RandomAccessFileWrapper;
import java.io.IOException;

/**
 *
 * @author TimePath
 */
class BlockAllocationTableHeader {

    /**
     * 8 * 4
     */
    static final long SIZE = 32;

    /**
     * Number of data blocks.
     */
    final int blockCount;

    /**
     * Number of data blocks that point to data.
     */
    final int blocksUsed;

    /**
     * Header checksum.
     * The checksum is simply the sum total of all the preceeding DWORDs in the header
     */
    final int checksum;

    /**
     *
     */
    final int dummy0;

    /**
     *
     */
    final int dummy1;

    /**
     *
     */
    final int dummy2;

    /**
     *
     */
    final int dummy3;

    /**
     *
     */
    final int lastBlockUsed;

    final long pos;

    BlockAllocationTableHeader(GCF g) throws IOException {
        RandomAccessFileWrapper raf = g.raf;
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

    @Override
    public String toString() {
        int checked = check();
        String checkState = (checksum == checked) ? "OK" : checksum + " vs " + checked;
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
