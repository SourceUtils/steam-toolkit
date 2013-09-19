package com.timepath.steam.io.storage;

import com.timepath.steam.io.storage.util.Archive;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class Files extends Archive {

    FilenameFilter norm = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return !name.endsWith(".vpk");
        }
        
    };
    
    private void recurse(File parent, FileEntry root) {
        File[] files = parent.listFiles(norm);
        if(files == null) {
            return;
        }
        for(File f : files) {
            FileEntry e = new FileEntry(f);
            root.add(e);
            recurse(f, e);
        }
    }

    class FileEntry extends DirectoryEntry {

        File f;

        FileEntry(File f) {
            this.f = f;
        }

        @Override
        public int getItemSize() {
            return (int) f.length();
        }

        @Override
        public Object getAttributes() {
            return null;
        }

        @Override
        public boolean isDirectory() {
            return f.isDirectory();
        }

        @Override
        public String getName() {
            return f.getName();
        }

        @Override
        public Archive getArchive() {
            return Files.this;
        }

        @Override
        public void extract(File dir) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public InputStream asStream() {
            try {
                return new BufferedInputStream(new FileInputStream(f));
            } catch(FileNotFoundException ex) {
                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        @Override
        public boolean isComplete() {
            return true;
        }

    }

    File f;

    FileEntry root;

    public Files(File dir) {
        f = dir;
        root = new FileEntry(f);
        recurse(f, root);
    }

    @Override
    public DirectoryEntry getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return f.getName();
    }

}
