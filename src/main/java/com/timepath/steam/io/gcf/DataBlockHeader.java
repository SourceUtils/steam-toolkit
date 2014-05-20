package com.timepath.steam.io.gcf;

import com.timepath.DataUtils;
import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;

/**
 * @author TimePath
 */
class DataBlockHeader {

    /**
     * Size of each data block in bytes.
     */
    final         int blockSize;
    /**
     * Offset to first data block.
     */
    final         int firstBlockOffset;
    /**
     * Number of data blocks.
     */
    private final int blockCount;
    /**
     * Number of data blocks that contain data.
     */
    private final int blocksUsed;
    /**
     * Header checksum.
     */
    private final int checksum;
    /**
     * GCF file version.
     */
    private final int gcfRevision;

    DataBlockHeader(RandomAccessFileWrapper raf) throws IOException {
        gcfRevision = raf.readULEInt();
        blockCount = raf.readULEInt();
        blockSize = raf.readULEInt();
        firstBlockOffset = raf.readULEInt();
        blocksUsed = raf.readULEInt();
        checksum = raf.readULEInt();
    }

    @Override
    public String toString() {
        int checked = 0;
        checked += DataUtils.updateChecksumAdd(blockCount);
        checked += DataUtils.updateChecksumAdd(blockSize);
        checked += DataUtils.updateChecksumAdd(firstBlockOffset);
        checked += DataUtils.updateChecksumAdd(blocksUsed);
        String checkState = ( checksum == checked ) ? "OK" : ( checksum + "vs" + checked );
        return "v:" + gcfRevision + ", blocks:" + blockCount + ", size:" + blockSize + ", offset:0x" +
               Integer.toHexString(firstBlockOffset) + ", used:" + blocksUsed + ", check:" + checkState;
    }

    int check() {
        int checked = 0;
        checked += DataUtils.updateChecksumAdd(blockCount);
        checked += DataUtils.updateChecksumAdd(blockSize);
        checked += DataUtils.updateChecksumAdd(firstBlockOffset);
        checked += DataUtils.updateChecksumAdd(blocksUsed);
        return checked;
    }
}
