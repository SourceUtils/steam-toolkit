package com.timepath.steam.io.storage;

import com.timepath.DataUtils;
import com.timepath.StringUtils;
import com.timepath.steam.io.storage.util.Archive;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import com.timepath.swing.TreeUtils;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Loads _dir.vpk files https://developer.valvesoftware.com/wiki/VPK_File_Format
 *
 * @author timepath
 */
public class VPK extends Archive {

    private static final Logger LOG = Logger.getLogger(VPK.class.getName());
    
    private static int HEADER = 0x55AA1234;

    private DefaultMutableTreeNode es = new DefaultMutableTreeNode();

    private ByteBuffer data;

    private String name;

    private boolean multiPart;

    private File[] store;

    private ByteBuffer[] mappings;

    private ByteBuffer getData(int i) {
        try {
            if(mappings[i] == null) {
                mappings[i] = DataUtils.mapFile(store[i]);
            }
            return mappings[i];
        } catch(IOException ex) {
            Logger.getLogger(VPK.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private ArrayList<VPKDirectoryEntry> entries;

    public VPK() {
    }

    public VPK loadArchive(final File file) {
        try {
            //<editor-fold defaultstate="collapsed" desc="Map extra archives">
            this.name = file.getName();
            this.name = this.name.substring(0, name.length() - 4); // Strip '.vkp'
            if(name.endsWith("_dir")) {
                multiPart = true;
                this.name = this.name.substring(0, name.length() - 4); // Strip '_dir'
            }
            File[] files = file.getParentFile().listFiles(new FileFilter() {
                public boolean accept(File f) {
                    if(f.equals(file)) {
                        return false;
                    }
                    if(f.getName().startsWith(name) && f.getName().length() == name.length() + 8) { // '_###.vpk' = 8

                        return true;
                    }
                    return false;
                }
            });
            store = new File[files.length];
            mappings = new ByteBuffer[store.length];
            for(File f : files) {
                String[] split = f.getName().split("_");
                int idx = Integer.parseInt(split[split.length - 1].replaceAll(".vpk", ""));
                store[idx] = f;
            }
            //</editor-fold>

            ByteBuffer b = DataUtils.mapFile(file);

            int signature = b.getInt();
            if(signature != HEADER) {
                return null;
            }
            int ver = b.getInt();
            int treeLength = b.getInt(); // unsigned length of directory slice

            int dataLength = 0;
            int v2 = 0;
            int v3 = 0; // 48 in most
            int v4 = 0;
            if(ver >= 2) {
                dataLength = b.getInt();
                v2 = b.getInt();
                v3 = b.getInt();
                v4 = b.getInt();
            }

            entries = new ArrayList<VPKDirectoryEntry>();
            VPKDirectoryEntry root = new VPKDirectoryEntry("/");
            root.isDirectory = true;
            entries.add(root);

            ByteBuffer directoryInfo = DataUtils.getSlice(b, treeLength);
            data = DataUtils.getSlice(b, dataLength);
            b.get(new byte[v2]); // dir
            b.get(new byte[v3]); // single+dir
            b.get(new byte[v4]); // dir

            Object[][] debug = {
                {"dataLength = ", dataLength},
                {"v2 = ", v2},
                {"v3 = ", v3},
                {"v4 = ", v4},
                {"Underflow = ", b.remaining()},};
            LOG.info(StringUtils.fromDoubleArray(debug, "Debug:"));

            parseTree(directoryInfo);

        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return this;
    }

    private void parseTree(ByteBuffer b) {
        for(;;) { // Extensions
            String extension = DataUtils.readZeroString(b);
            if(extension.length() == 0) { // End of data
                break;
            }
            if(extension.equals(" ")) { // No extension
                extension = null;
            }
            for(;;) { // Paths
                String path = DataUtils.readZeroString(b);
                if(path.length() == 0) {
                    break;
                }
                DefaultMutableTreeNode p = nodeForPath(path);
                for(;;) { // File names
                    String filename = DataUtils.readZeroString(b);
                    if(filename.length() == 0) {
                        break;
                    }
                    VPKDirectoryEntry e = readFileInfo(b,
                                                       filename + (extension != null ? ("." + extension) : ""));
                    entries.add(e);
                    p.add(new DefaultMutableTreeNode(e));
                }
            }
        }
    }

    private DefaultMutableTreeNode nodeForPath(String path) {
        DefaultMutableTreeNode parent = es;
        if(!path.equals(" ")) {
            String[] components = path.split("/");
//            LOG.log(Level.INFO, "Path: {0}, {1}", new Object[] {path, Arrays.toString(components)});
            for(String part : components) {
                DefaultMutableTreeNode node = null;
                Enumeration e = parent.children();
                while(e.hasMoreElements()) {
                    DefaultMutableTreeNode n = ((DefaultMutableTreeNode) e.nextElement());
                    if(n.getUserObject().toString().equalsIgnoreCase(part)) {
                        node = n;
                        break;
                    }
                }
                if(node == null) {
                    VPKDirectoryEntry dirEntry = new VPKDirectoryEntry(part);
                    dirEntry.isDirectory = true;
                    node = new DefaultMutableTreeNode(dirEntry);
                    parent.add(node);
                }
                parent = node;
            }
        }
        return parent;
    }

    private VPKDirectoryEntry readFileInfo(ByteBuffer b, String name) {
        VPKDirectoryEntry e = new VPKDirectoryEntry(name);

        e.CRC = (long) b.getInt() & 0xFFFFFFFFL;
        e.preloadBytes = b.getShort();
        e.archiveIndex = b.getShort();
        e.entryOffset = b.getInt();
        e.entryLength = b.getInt();

        b.position(b.position() + e.preloadBytes); // TODO: load preload bytes

        ByteBuffer source;
        if(e.archiveIndex == 0x7FFF) { // This archive
            source = data;
        } else {
            source = getData(e.archiveIndex);
        }
        source.position(e.entryOffset);
        e.data = DataUtils.getSlice(source, e.entryLength);
        short term = b.getShort();
        assert term == 0xFFFF : "VPK directory reading failed";
        return e;
    }

    public InputStream get(int index) {
        return null;
    }

    /**
     * If a file contains preload data, the preload data immediately follows the
     * above structure. The entire size of a file is PreloadBytes + EntryLength.
     */
    class VPKDirectoryEntry extends DirectoryEntry {

        /**
         * A 32bit CRC of the file's data.
         */
        long CRC;

        short preloadBytes;

        short archiveIndex;

        int entryOffset;

        int entryLength;

        ByteBuffer data;

        String name;

        boolean isDirectory;

        VPKDirectoryEntry() {
        }

        VPKDirectoryEntry(String name) {
            this.name = name;
        }

        public long getChecksum() {
            return CRC;
        }

        public long calculateChecksum() {
            if(data == null) {
                return -1;
            }
            CRC32 crc = new CRC32();
            byte[] buf = new byte[4096];
            data.position(0);
            while(data.hasRemaining()) {
                int bsize = Math.min(buf.length, data.remaining());
                data.get(buf, 0, bsize);
                crc.update(buf, 0, bsize);
            }

            return crc.getValue();
        }

        public int getItemSize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object getAttributes() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public String getPath() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Archive getArchive() {
            return VPK.this;
        }

        public boolean isComplete() {
            long theoretical = CRC;
            long real = calculateChecksum();
            return theoretical == real;
        }

        public String getName() {
            return name;
        }

        public String getAbsoluteName() {
            return getName(); // TODO
        }

        public DirectoryEntry[] getImmediateChildren() {
            return null;
        }

        public int getIndex() {
            return -1;
        }

        public void extract(File dir) throws IOException {
            File out = new File(dir, this.name);
            if(this.isDirectory) {
                out.mkdir();
                for(DirectoryEntry e : getImmediateChildren()) {
                    e.extract(out);
                }
            } else {
                out.createNewFile();
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[4096];
                data.position(0);
                while(data.hasRemaining()) {
                    int bsize = Math.min(buf.length, data.remaining());
                    data.get(buf, 0, bsize);
                    fos.write(buf, 0, bsize);
                }
            }
        }
    }

    public ArrayList<DirectoryEntry> find(String search) {
        ArrayList<DirectoryEntry> list = new ArrayList<DirectoryEntry>();
        return list;
    }

    public DirectoryEntry getRoot() {
        return entries.get(0);
    }

    public void analyze(DefaultMutableTreeNode top, boolean leaves) {
        TreeUtils.moveChildren(es, top);
    }

    @Override
    public String toString() {
        return this.name;
    }

}
