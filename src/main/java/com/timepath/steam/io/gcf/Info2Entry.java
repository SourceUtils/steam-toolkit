package com.timepath.steam.io.gcf;

import com.timepath.io.RandomAccessFileWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author TimePath
 */
class Info2Entry {

    /**
     * 1 * 4
     */
    static final long SIZE = 4;
    private final int Dummy0;

    Info2Entry(@NotNull RandomAccessFileWrapper raf) throws IOException {
        Dummy0 = raf.readULEInt();
    }

    @NotNull
    @Override
    public String toString() {
        return String.valueOf(Dummy0);
    }
}
