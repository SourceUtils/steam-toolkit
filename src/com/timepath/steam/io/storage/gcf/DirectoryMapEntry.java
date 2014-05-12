package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;

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

    DirectoryMapEntry(RandomAccessFileWrapper raf) throws IOException {
        firstBlockIndex = raf.readULEInt();
    }
}
