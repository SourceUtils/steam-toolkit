package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
class DirectoryMapEntry {

    /**
     * 1 * 4
     */
    static final long SIZE = 4;
    /**
     * Index of the first data block. (N/A if == BlockCount.)
     */
    final int firstBlockIndex;

    DirectoryMapEntry(@NotNull RandomAccessFileWrapper raf) throws IOException {
        firstBlockIndex = raf.readULEInt();
    }
}
