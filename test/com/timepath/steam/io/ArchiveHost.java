package com.timepath.steam.io;

import com.timepath.steam.io.storage.ACF;
import com.timepath.steam.io.storage.util.Archive;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import com.timepath.vfs.VFSStub;
import com.timepath.vfs.VFile;
import com.timepath.vfs.ftp.FTPFS;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author timepath
 */
public class ArchiveHost {

    private static class ArchiveFile extends VFile {

        DirectoryEntry de;

        public ArchiveFile(DirectoryEntry e) {
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

    public static void addAll(DirectoryEntry e, VFile f) {
        for(DirectoryEntry de : e.children()) {
            VFile vf = new ArchiveFile(de);
            f.add(vf);
            addAll(de, vf);
        }
    }

    public static void main(String... args) throws IOException {
        int appID = 440;
        ACF acf = new ACF(appID);
        FTPFS ftp = new FTPFS(8000);
        VFile f = new VFSStub(appID + "");
        for(Archive a : acf.getArchives()) {
            VFile ar = new VFSStub(a.toString());
            addAll(a.getRoot(), ar);
            f.add(ar);
        }
        ftp.add(f);
        ftp.run();
    }

}
