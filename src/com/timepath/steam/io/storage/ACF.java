package com.timepath.steam.io.storage;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author timepath
 */
public class ACF implements Archive {

    public static void main(String... args) {
        new ACF().loadArchive(440);
    }

    public Archive loadArchive(int appID) {
        return loadArchive(new File(SteamUtils.getSteamApps(), "appmanifest_" + appID + ".acf"));
    }
    private File root;

    @Override
    public Archive loadArchive(File manifest) {
        try {
            VDF v = new VDF();
            v.readExternal(new FileInputStream(manifest));
            root = new File(v.getRoot().get("AppState").get("UserConfig").get("appinstalldir").getValue());
            // TODO: gameinfo.txt
            ArrayList<File> files = buildPaths(root);
            LOG.log(Level.INFO, "VPK files: {0}", Arrays.toString(files.toArray()));
            for(File f : files) {
                VPK.add(new VPK().loadArchive(f));
            }
            return this;
        } catch(FileNotFoundException ex) {
            Logger.getLogger(ACF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private ArrayList<VPK> VPK = new ArrayList<VPK>();

    private ArrayList<File> buildPaths(File path) {
        File[] files = path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (!file.isDirectory() && file.getName().toLowerCase().endsWith("_dir.vpk"));
            }
        });
        File[] directories = path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        ArrayList<File> arr = new ArrayList<File>();
        if(files != null) {
            arr.addAll(Arrays.asList(files));
        }
        if(directories != null) {
            for(File d : directories) {
                arr.addAll(buildPaths(d));
            }
        }
        return arr;
    }

    public InputStream get(String path) {
        File file = new File(root, path);
        try {
            return new FileInputStream(file);
        } catch(FileNotFoundException ex) {
            Logger.getLogger(ACF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null; // check VPK files
    }

    @Override
    public InputStream get(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<DirectoryEntry> find(String search) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DirectoryEntry getRoot() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void analyze(DefaultMutableTreeNode top, boolean leaves) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    private static final Logger LOG = Logger.getLogger(ACF.class.getName());
}
