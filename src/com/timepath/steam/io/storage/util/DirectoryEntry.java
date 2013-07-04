package com.timepath.steam.io.storage.util;

import com.timepath.io.utils.ViewableData;
import java.io.File;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 *
 * @author timepath
 */
public abstract class DirectoryEntry implements ViewableData {

    public abstract int getItemSize();

    public abstract Object getAttributes();

    public abstract boolean isDirectory();

    public abstract String getName();

    public abstract String getPath();

    public abstract String getAbsoluteName();

    public abstract Archive getArchive();

    public abstract DirectoryEntry[] getImmediateChildren();

    public abstract int getIndex();

    public abstract void extract(File dir) throws IOException;
    
    public abstract boolean isComplete();

    public long getChecksum() {
        return -1;
    }

    public long calculateChecksum() {
        return -1;
    }

    public Icon getIcon() {
        if(getIndex() == 0) {
            return getArchive().getIcon();
        }
        if(isDirectory()) {
            return UIManager.getIcon("FileView.directoryIcon");
        } else if(!isComplete()) {
            return UIManager.getIcon("html.missingImage");
        } else {
            return UIManager.getIcon("FileView.fileIcon");
        }
    }

    @Override
    public String toString() {
        return getName();
    }

}