package com.timepath.steam.io;

import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import com.timepath.vfs.VFile;
import com.timepath.vfs.ftp.FTPFS;
import java.io.IOException;
import java.io.InputStream;

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

        public ArchiveFile(DirectoryEntry e) {
            for(DirectoryEntry de : e.children()) {
                VFile vf = new ArchiveFile(de);
                this.add(vf);
            }
            this.de = e;
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

    public static void main(String... args) throws IOException {
        int appID = 440;
        ACF acf = ACF.fromManifest(appID);
        FTPFS ftp = new FTPFS(8000);
        ftp.addAll(new ArchiveFile(acf.getRoot()).list());
        ftp.run();
    }

}
