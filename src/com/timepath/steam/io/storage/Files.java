package com.timepath.steam.io.storage;

import com.timepath.steam.io.storage.util.ExtendedVFile;
import com.timepath.vfs.SimpleVFile;
import java.io.*;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class Files extends ExtendedVFile {

    private static final Logger LOG = Logger.getLogger(Files.class.getName());

    /**
     * Archive to directory map
     */
    private final HashMap<ExtendedVFile, ExtendedVFile> archives = new HashMap<ExtendedVFile, ExtendedVFile>();

    private final File f;

    public Files(File f) {
        this.f = f;
        this.insert(f);
    }

    @Override
    public Object getAttributes() {
        return null;
    }

    @Override
    public String getName() {
        return f.getName();
    }

    @Override
    public ExtendedVFile getRoot() {
        return this;
    }

    public void insert(File f) {
        walk(f, this);
        // TODO: additive directories to avoid this kludge
        for(Entry<ExtendedVFile, ExtendedVFile> e : archives.entrySet()) {
            merge(e.getKey(), e.getValue());
        }
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return f.isDirectory();
    }

    @Override
    public long lastModified() {
        return f.lastModified();
    }

    @Override
    public long length() {
        return f.length();
    }

    @Override
    public InputStream stream() {
        try {
            return new BufferedInputStream(new FileInputStream(f));
        } catch(FileNotFoundException ex) {
            Logger.getLogger(SimpleVFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String toString() {
        return f.getName();
    }

    private void merge(SimpleVFile r, SimpleVFile parent) {
        for(SimpleVFile d : r.children()) {
            SimpleVFile existing = null;
            for(SimpleVFile t : parent.children()) {
                if(t.getName().equals(d.getName())) {
                    existing = t;
                    break;
                }
            }
            if(existing == null) {
                parent.copy(d);
            } else {
                merge(d, existing);
            }
        }
    }

    private void walk(File dir, Files parent) {
        File[] ls = dir.listFiles();
        if(ls == null) {
            return;
        }
        for(File file : ls) {
            if(file.getName().endsWith("_dir.vpk")) {
                VPK v = VPK.loadArchive(file);
                archives.put(v.getRoot(), parent);
            } else if(file.getName().endsWith(".vpk")) {
                // TODO:
            } else {
                Files e = new Files(file);
                parent.add(e);
                walk(file, e);
            }
        }
    }

}
