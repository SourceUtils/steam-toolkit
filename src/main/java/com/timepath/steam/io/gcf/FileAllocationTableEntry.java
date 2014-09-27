package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
class FileAllocationTableEntry {

    /**
     * 1 * 4
     */
    static final long SIZE = 4;
    /**
     * The index of the next data block.
     * If == FileAllocationTableHeader.isLongTerminator, there are no more clusters in the file
     */
    final int nextClusterIndex;

    FileAllocationTableEntry(@NotNull RandomAccessFileWrapper raf) throws IOException {
        nextClusterIndex = raf.readULEInt();
    }

    @NotNull
    @Override
    public String toString() {
        return "nextDataBlockIndex:" + nextClusterIndex;
    }
}
