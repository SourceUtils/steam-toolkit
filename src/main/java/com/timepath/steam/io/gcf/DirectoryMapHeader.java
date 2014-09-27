package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
class DirectoryMapHeader {

    /**
     * 2 * 4
     */
    static final long SIZE = 8;
    final long pos;
    /**
     * Always 0x00000000
     */
    private final int dummy0;
    /**
     * Always 0x00000001
     */
    private final int headerVersion;

    DirectoryMapHeader(@NotNull GCF g) throws IOException {
        @NotNull RandomAccessFileWrapper raf = g.raf;
        pos = raf.getFilePointer();
        headerVersion = raf.readULEInt();
        dummy0 = raf.readULEInt();
        g.directoryMapEntries = new DirectoryMapEntry[g.manifestHeader.nodeCount];
        raf.skipBytes(g.directoryMapEntries.length * DirectoryMapEntry.SIZE);
    }

    @NotNull
    @Override
    public String toString() {
        return "headerVersion:" + headerVersion + ", Dummy0:" + dummy0;
    }
}
