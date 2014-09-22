package com.timepath.steam.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class VDF {

    public static boolean isBinary(File f) {
        try {
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            rf.seek(rf.length() - 1);
            int r = rf.read();
            return (r == 0x00) || (r == 0x08) || (r == 0xFF);
        } catch (IOException ex) {
            Logger.getLogger(VDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public static VDFNode load(File f) throws IOException {
        return load(new FileInputStream(f));
    }

    public static VDFNode load(InputStream is) throws IOException {
        return load(is, StandardCharsets.UTF_8);
    }

    public static VDFNode load(InputStream is, Charset c) throws IOException {
        return new VDFNode(is, c);
    }

    public static VDFNode load(File f, Charset c) throws IOException {
        return load(new FileInputStream(f), c);
    }
}
