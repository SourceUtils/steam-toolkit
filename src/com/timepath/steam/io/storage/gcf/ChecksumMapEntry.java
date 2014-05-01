package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import java.io.IOException;

/**
 *
 * @author TimePath
 */
public class ChecksumMapEntry {

    /**
     * 2 * 4
     */
    static final long SIZE = 8;

    /**
     * Number of checksums.
     */
    final int checksumCount;

    /**
     * Index of first checksum.
     */
    final int firstChecksumIndex;

    ChecksumMapEntry(RandomAccessFileWrapper raf) throws IOException {
        checksumCount = raf.readULEInt();
        firstChecksumIndex = raf.readULEInt();
    }

    @Override
    public String toString() {
        return "checkCount:" + checksumCount + ", first:" + firstChecksumIndex;
    }

}
