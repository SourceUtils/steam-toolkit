package com.timepath.steam.io.storage.util;

import com.timepath.io.utils.ViewableData;
import com.timepath.swing.TreeUtils;
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

    public void analyze(DefaultMutableTreeNode top, boolean leaves) {
        if(top.getUserObject() instanceof DirectoryEntry) {
            DirectoryEntry e = (DirectoryEntry) top.getUserObject();
            for(DirectoryEntry n : e.children()) {
                DefaultMutableTreeNode ret = new DefaultMutableTreeNode(n);
                if(n.isDirectory()) {
                    analyze(ret, leaves);
                    top.add(ret);
                } else if(leaves) {
                    top.add(ret);
                }
            }
        } else {
            DefaultMutableTreeNode ret = new DefaultMutableTreeNode(getRoot());
            analyze(ret, leaves);
            TreeUtils.moveChildren(ret, top);
        }
    }

    public Icon getIcon() {
        Icon i = UIManager.getIcon("FileView.hardDriveIcon");
        if(i == null) {
            i = UIManager.getIcon("FileView.directoryIcon");
        }
        return i;
    }

}
