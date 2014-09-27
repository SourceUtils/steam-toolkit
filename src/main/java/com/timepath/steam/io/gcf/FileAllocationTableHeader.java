package com.timepath.steam.io.gcf;

import com.timepath.DataUtils;
import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
class FileAllocationTableHeader {

    /**
     * 4 * 4
     */
    static final long SIZE = 16;
    final long pos;
    /**
     * Header checksum.
     */
    private final int checksum;
    /**
     * Number of data blocks.
     */
    private final int clusterCount;
    /**
     * Index of 1st unused GCFFRAGMAP entry.
     */
    private final int firstUnusedEntry;
    /**
     * Defines the end of block chain terminator.
     * If the value is 0, then the terminator is 0x0000FFFF; if the value is 1, then the
     * terminator is 0xFFFFFFFF
     */
    private final int isLongTerminator;

    FileAllocationTableHeader(@NotNull GCF g) throws IOException {
        @NotNull RandomAccessFileWrapper raf = g.raf;
        pos = raf.getFilePointer();
        clusterCount = raf.readULEInt();
        firstUnusedEntry = raf.readULEInt();
        isLongTerminator = raf.readULEInt();
        checksum = raf.readULEInt();
        g.fragMapEntries = new FileAllocationTableEntry[clusterCount];
        raf.skipBytes(g.fragMapEntries.length * FileAllocationTableEntry.SIZE);
    }

    @NotNull
    @Override
    public String toString() {
        int checked = check();
        @NotNull String checkState = (checksum == checked) ? "OK" : (checksum + "vs" + checked);
        return "blockCount:" + clusterCount + ", firstUnusedEntry:" + firstUnusedEntry + ", isLongTerminator:" +
                isLongTerminator + ", checksum:" + checkState;
    }

    int check() {
        int checked = 0;
        checked += DataUtils.updateChecksumAdd(clusterCount);
        checked += DataUtils.updateChecksumAdd(firstUnusedEntry);
        checked += DataUtils.updateChecksumAdd(isLongTerminator);
        return checked;
    }
}
