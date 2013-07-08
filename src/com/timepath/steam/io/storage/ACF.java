package com.timepath.steam.io.storage;

import com.timepath.steam.io.storage.util.Archive;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
public class ACF extends Archive {    

    public ACF(File manifest) throws FileNotFoundException {
        VDF v = new VDF();
        v.readExternal(new FileInputStream(manifest));
        root = new File(v.getRoot().get("AppState").get("UserConfig").get("appinstalldir").getValue());
        // TODO: gameinfo.txt
        ArrayList<File> files = buildPaths(root);
        LOG.log(Level.INFO, "VPK files: {0}", Arrays.toString(files.toArray()));
        for(File f : files) {
            VPK.add(new VPK(f));
        }
    }
    
    public ACF(int appID) throws FileNotFoundException {
        this(new File(SteamUtils.getSteamApps(), "appmanifest_" + appID + ".acf"));
    }
    
    private File root;

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
    public DirectoryEntry getRoot() {
        return new ACFDirectoryEntry("/");
    }

    class ACFDirectoryEntry extends DirectoryEntry {

        String name;

        ACFDirectoryEntry(String name) {
            this.name = name;
        }

        @Override
        public int getItemSize() {
            return 1;
        }

        @Override
        public Object getAttributes() {
            return null;
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public String getName() {
            return "/";
        }

        @Override
        public Archive getArchive() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public void extract(File out) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long getChecksum() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long calculateChecksum() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public InputStream asStream() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    @Override
    public void analyze(DefaultMutableTreeNode top, boolean leaves) {
        for(VPK v : VPK) {
            DefaultMutableTreeNode archive = new DefaultMutableTreeNode(v);
            v.analyze(archive, leaves);
            top.add(archive);
        }
    }

    private static final Logger LOG = Logger.getLogger(ACF.class.getName());

}
