package com.timepath.steam.io.storage.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author timepath
 */
public interface Archive {

    public Archive loadArchive(File f);

    public InputStream get(int index);

    public ArrayList<DirectoryEntry> find(String search);

    public DirectoryEntry getRoot();

    public void analyze(DefaultMutableTreeNode top, boolean leaves);
}
