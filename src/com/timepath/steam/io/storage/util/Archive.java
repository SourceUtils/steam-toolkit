package com.timepath.steam.io.storage.util;

import com.timepath.io.utils.ViewableData;
import java.io.InputStream;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author timepath
 */
public abstract class Archive implements ViewableData {

    public abstract InputStream get(int index);

    public abstract ArrayList<DirectoryEntry> find(String search);

    public abstract DirectoryEntry getRoot();

    public abstract void analyze(DefaultMutableTreeNode top, boolean leaves);

    public Icon getIcon() {
        Icon i = UIManager.getIcon("FileView.hardDriveIcon");
        if(i == null) {
            i = UIManager.getIcon("FileView.directoryIcon");
        }
        return i;
    }

}
