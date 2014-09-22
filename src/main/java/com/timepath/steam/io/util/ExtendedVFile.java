package com.timepath.steam.io.util;

import com.timepath.swing.TreeUtils;
import com.timepath.vfs.SimpleVFile;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public abstract class ExtendedVFile extends SimpleVFile {

    private static final Logger LOG = Logger.getLogger(ExtendedVFile.class.getName());

    protected ExtendedVFile() {
    }

    public void analyze(DefaultMutableTreeNode top, boolean leaves) {
        if (top.getUserObject() instanceof ExtendedVFile) { // the root node has been added
            ExtendedVFile e = (ExtendedVFile) top.getUserObject();
            for (SimpleVFile n : e.list()) {
                DefaultMutableTreeNode ret = new DefaultMutableTreeNode(n);
                if (n.isDirectory()) {
                    analyze(ret, leaves);
                    top.add(ret);
                } else if (leaves) {
                    top.add(ret);
                }
            }
        } else { // the root node has not been added
            DefaultMutableTreeNode ret = new DefaultMutableTreeNode(this);
            analyze(ret, leaves);
            TreeUtils.moveChildren(ret, top);
        }
    }

    public long calculateChecksum() {
        return -1;
    }

    public String getAbsoluteName() {
        return getPath() + getName();
    }

    public abstract Object getAttributes();

    public long getChecksum() {
        return -1;
    }

    public abstract ExtendedVFile getRoot();

    public abstract boolean isComplete();
}
