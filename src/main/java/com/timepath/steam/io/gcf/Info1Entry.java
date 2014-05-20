package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;

/**
 * @author TimePath
 */
class Info1Entry {

    /**
     * 1 * 4
     */
    static final long SIZE = 4;
    private final int Dummy0;

    Info1Entry(RandomAccessFileWrapper raf) throws IOException {
        Dummy0 = raf.readULEInt();
    }

    @Override
    public String toString() {
        return String.valueOf(Dummy0);
    }
}
