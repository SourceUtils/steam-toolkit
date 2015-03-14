package com.timepath.steam.io;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class VDF {

    public static boolean isBinary(@NotNull File f) {
        try {
            @NotNull RandomAccessFile rf = new RandomAccessFile(f, "r");
            rf.seek(rf.length() - 1);
            int r = rf.read();
            return (r == 0x00) || (r == 0x08) || (r == 0xFF);
        } catch (IOException ex) {
            Logger.getLogger(VDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @NotNull
    public static VDFNode load(@NotNull File f) throws IOException {
        return load(new FileInputStream(f));
    }

    @NotNull
    public static VDFNode load(@NotNull InputStream is) throws IOException {
        return load(is, StandardCharsets.UTF_8);
    }

    @NotNull
    public static VDFNode load(@NotNull InputStream is, @NotNull Charset c) throws IOException {
        return new VDFNode(is, c);
    }

    @NotNull
    public static VDFNode load(@NotNull File f, @NotNull Charset c) throws IOException {
        return load(new FileInputStream(f), c);
    }
}
