package com.timepath.steam.io.storage.gcf;

import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;

/**
 * @author TimePath
 */
class ChecksumHeader {

    /**
     * The number of bytes in the checksum section (excluding this structure
     * and the following LatestApplicationVersion structure).
     */
    private final int checksumSize;
    /**
     * Always 0x00000001.
     */
    private final int headerVersion;

    ChecksumHeader(RandomAccessFileWrapper raf) throws IOException {
        headerVersion = raf.readULEInt();
        checksumSize = raf.readULEInt();
    }

    @Override
    public String toString() {
        return "headerVersion:" + headerVersion + ", ChecksumSize:" + checksumSize;
    }
}
