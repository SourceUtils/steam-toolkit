package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

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

    ChecksumEntry(@NotNull RandomAccessFileWrapper raf) throws IOException {
        checksum = raf.readULEInt();
    }

    @NotNull
    @Override
    public String toString() {
        return "check:" + checksum;
    }
}
