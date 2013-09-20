package com.timepath.steam.io;

import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import com.timepath.vfs.VFile;
import com.timepath.vfs.ftp.FTPFS;
import com.timepath.vfs.http.HTTPFS;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class ArchiveHost {

    /**
     * TODO: Merge valve package formats with same interface
     */
    public static class ArchiveFile extends VFile {

        DirectoryEntry de;

        public ArchiveFile(DirectoryEntry de) {
            for(DirectoryEntry e : de.children()) {
                VFile vf = new ArchiveFile(e);
                this.add(vf);
            }
            this.de = de;
        }

        @Override
        public boolean isDirectory() {
            return de.isDirectory();
        }

        @Override
        public String owner() {
            return "ftp";
        }

        @Override
        public String group() {
            return "ftp";
        }

        @Override
        public long fileSize() {
            return de.getItemSize();
        }

        @Override
        public long modified() {
            return System.currentTimeMillis();
        }

        @Override
        public String path() {
            return "/" + de.getPath();
        }

        @Override
        public String name() {
            return de.getName();
        }

        @Override
        public InputStream content() {
            return de.asStream();
        }

    }

    public static void main(String... args) {
        int appID = 440;
        try {
            final ACF acf = ACF.fromManifest(appID);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        HTTPFS http = new HTTPFS(8080);
                        http.addAll(new ArchiveFile(acf.getRoot()).list());
                        http.run();
                    } catch(IOException ex) {
                        Logger.getLogger(ArchiveHost.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start();
            try {
                FTPFS ftp = new FTPFS(2121);
                ftp.addAll(new ArchiveFile(acf.getRoot()).list());
                ftp.run();
            } catch(IOException ex) {
                Logger.getLogger(ArchiveHost.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch(FileNotFoundException ex) {
            Logger.getLogger(ArchiveHost.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
