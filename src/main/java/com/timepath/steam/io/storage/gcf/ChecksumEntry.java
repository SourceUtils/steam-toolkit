package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;

/**
 * @author TimePath
 */
class ChecksumEntry {

    /**
     * 1 * 4
     */
    static final long SIZE = 4;
    /**
     * Checksum.
     */
    private final int checksum;

    ChecksumEntry(RandomAccessFileWrapper raf) throws IOException {
        checksum = raf.readULEInt();
    }

    @Override
    public String toString() {
        return "check:" + checksum;
    }
}
