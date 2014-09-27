package com.timepath.steam.io.util;

import com.timepath.swing.TreeUtils;
import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public abstract class ExtendedVFile extends SimpleVFile {

    private static final Logger LOG = Logger.getLogger(ExtendedVFile.class.getName());

    protected ExtendedVFile() {
    }

    public void analyze(@NotNull DefaultMutableTreeNode top, boolean leaves) {
        if (top.getUserObject() instanceof ExtendedVFile) { // the root node has been added
            @NotNull ExtendedVFile e = (ExtendedVFile) top.getUserObject();
            for (@NotNull SimpleVFile n : e.list()) {
                @NotNull DefaultMutableTreeNode ret = new DefaultMutableTreeNode(n);
                if (n.isDirectory()) {
                    analyze(ret, leaves);
                    top.add(ret);
                } else if (leaves) {
                    top.add(ret);
                }
            }
        } else { // the root node has not been added
            @NotNull DefaultMutableTreeNode ret = new DefaultMutableTreeNode(this);
            analyze(ret, leaves);
            TreeUtils.moveChildren(ret, top);
        }
    }

    public long calculateChecksum() {
        return -1;
    }

    @NotNull
    public String getAbsoluteName() {
        return getPath() + getName();
    }

    @Nullable
    public abstract Object getAttributes();

    public long getChecksum() {
        return -1;
    }

    public abstract ExtendedVFile getRoot();

    public abstract boolean isComplete();
}
