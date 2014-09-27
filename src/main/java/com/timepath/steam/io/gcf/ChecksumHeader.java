package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

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

    ChecksumHeader(@NotNull RandomAccessFileWrapper raf) throws IOException {
        headerVersion = raf.readULEInt();
        checksumSize = raf.readULEInt();
    }

    @NotNull
    @Override
    public String toString() {
        return "headerVersion:" + headerVersion + ", ChecksumSize:" + checksumSize;
    }
}
