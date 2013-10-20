package com.timepath.steam.io.util;

import com.timepath.steam.io.storage.util.DirectoryEntry;
import com.timepath.vfs.VFile;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class VFileDirectoryEntryAdapter extends VFile {

    private static final Logger LOG = Logger.getLogger(VFileDirectoryEntryAdapter.class.getName());

    private final DirectoryEntry de;

    private final long modtime;

    public VFileDirectoryEntryAdapter(DirectoryEntry de) {
        for(DirectoryEntry e : de.children()) {
            VFile vf = new VFileDirectoryEntryAdapter(e);
            this.add(vf);
        }
        this.de = de;
        this.modtime = System.currentTimeMillis();
    }

    @Override
    public InputStream content() {
        return de.asStream();
    }

    @Override
    public long fileSize() {
        return de.getItemSize();
    }

    @Override
    public String group() {
        return owner();
    }

    @Override
    public boolean isDirectory() {
        return de.isDirectory();
    }

    @Override
    public long modified() {
        return modtime;
    }

    @Override
    public String name() {
        return de.getName();
    }

    @Override
    public String owner() {
        return System.getProperty("user.name", "nobody");
    }

    @Override
    public String path() {
        return "/" + de.getPath();
    }

}
