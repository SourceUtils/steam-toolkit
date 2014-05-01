package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import java.io.IOException;

/**
 *
 * @author TimePath
 */
class tagGCFDIRECTORYINFO2ENTRY {

    /**
     * 1 * 4
     */
    static final long SIZE = 4;

    final int Dummy0;

    tagGCFDIRECTORYINFO2ENTRY(RandomAccessFileWrapper raf) throws IOException {
        Dummy0 = raf.readULEInt();
    }

    @Override
    public String toString() {
        return "" + Dummy0;
    }

}
