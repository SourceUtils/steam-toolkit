package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
class ChecksumMapEntry {

    /**
     * 2 * 4
     */
    static final long SIZE = 8;
    /**
     * Number of checksums.
     */
    private final int checksumCount;
    /**
     * Index of first checksum.
     */
    private final int firstChecksumIndex;

    ChecksumMapEntry(@NotNull RandomAccessFileWrapper raf) throws IOException {
        checksumCount = raf.readULEInt();
        firstChecksumIndex = raf.readULEInt();
    }

    @NotNull
    @Override
    public String toString() {
        return "checkCount:" + checksumCount + ", first:" + firstChecksumIndex;
    }
}
