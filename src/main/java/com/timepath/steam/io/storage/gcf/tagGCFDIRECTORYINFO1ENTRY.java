package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;

/**
 * @author TimePath
 */
class tagGCFDIRECTORYINFO1ENTRY {

    /**
     * 1 * 4
     */
    static final long SIZE = 4;
    private final int Dummy0;

    tagGCFDIRECTORYINFO1ENTRY(RandomAccessFileWrapper raf) throws IOException {
        Dummy0 = raf.readULEInt();
    }

    @Override
    public String toString() {
        return String.valueOf(Dummy0);
    }
}
