package com.timepath.steam.io.storage;

import com.timepath.steam.io.storage.util.Archive;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import java.io.*;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class Files extends Archive {

    private static final Logger LOG = Logger.getLogger(Files.class.getName());

    /**
     * Archive to directory
     */
    private final HashMap<DirectoryEntry, DirectoryEntry> archives = new HashMap<DirectoryEntry, DirectoryEntry>();

    File dir;

    FileEntry root;

    public Files() {
    }

        public Files(File dir) {
            this.dir = dir;
        root = new FileEntry(dir);
        walk(dir, root);
        // TODO: additive directories to avoid this kludge
        Set<Entry<DirectoryEntry, DirectoryEntry>> a = archives.entrySet();
        for(Entry<DirectoryEntry, DirectoryEntry> e : a) {
            DirectoryEntry r = e.getKey();
            DirectoryEntry parent = e.getValue();
            merge(r, parent);
        }
    }

    @Override
    public DirectoryEntry getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return dir.getName();
    }

    private void walk(File dir, FileEntry parent) {
        File[] files = dir.listFiles();
        if(files == null) {
            return;
        }
        for(File file : files) {
            if(file.getName().endsWith("_dir.vpk")) {
                VPK v = new VPK(file);
                archives.put(v.getRoot(), parent);
            } else if(file.getName().endsWith(".vpk")) {
                // TODO:
            } else {
                FileEntry e = new FileEntry(file);
                parent.add(e);
                walk(file, e);
            }
        }
    }

    private void merge(DirectoryEntry r, DirectoryEntry parent) {
        for(DirectoryEntry d : r.children().toArray(new DirectoryEntry[r.children().size()])) {
            DirectoryEntry existing = null;
            for(DirectoryEntry t : parent.children()) {
                if(t.getName().equals(d.getName())) {
                    existing = t;
                }
            }
            if(existing == null) {
                parent.add(d);
            } else {
                merge(d, existing);
            }
        }
    }

    public class FileEntry extends DirectoryEntry {

        File file;

        FileEntry(File f) {
            this.file = f;
        }

        @Override
        public int getItemSize() {
            return (int) file.length();
        }

        @Override
        public Object getAttributes() {
            return null;
        }

        @Override
        public boolean isDirectory() {
            return file.isDirectory();
        }

        @Override
        public String getName() {
            return file.getName();
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
                return new BufferedInputStream(new FileInputStream(file));
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

}
