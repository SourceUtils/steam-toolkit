package com.timepath.steam.io.storage.util;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author timepath
 */
public interface DirectoryEntry {

    public int getItemSize();

    public Object getAttributes();

    public boolean isDirectory();

    public String getPath();

    public String getName();

    public String getAbsoluteName();

    public Archive getArchive();

    public boolean isComplete();

    public DirectoryEntry[] getImmediateChildren();

    public int getIndex();

    public void extract(File out) throws IOException;

}