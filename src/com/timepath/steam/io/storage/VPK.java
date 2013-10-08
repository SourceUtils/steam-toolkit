package com.timepath.steam.io.storage;

import com.timepath.DataUtils;
import com.timepath.StringUtils;
import com.timepath.io.ByteBufferInputStream;
import com.timepath.steam.io.storage.util.Archive;
import com.timepath.steam.io.storage.util.DirectoryEntry;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * Loads _dir.vpk files https://developer.valvesoftware.com/wiki/VPK_File_Format
 *
 * @author timepath
 */
public class VPK extends Archive {

    private static final Logger LOG = Logger.getLogger(VPK.class.getName());

    private static int HEADER = 0x55AA1234;

    private ByteBuffer globaldata;

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

    public VPK() {
    }

    public VPK(final File file) {
        VPK.loadArchive(this, file);
    }

    public VPKDirectoryEntry create(String name) {
        return new VPKDirectoryEntry(name);
    }

    public static VPK loadArchive(final File file) {
        return loadArchive(new VPK(), file);
    }

    public static VPK loadArchive(final VPK v, final File file) {
        try {
            //<editor-fold defaultstate="collapsed" desc="Map extra archives">
            v.name = file.getName();
            v.name = v.name.substring(0, v.name.length() - 4); // Strip '.vkp'
            if(v.name.endsWith("_dir")) {
                v.multiPart = true;
                v.name = v.name.substring(0, v.name.length() - 4); // Strip '_dir'
            }
            File[] files = file.getParentFile().listFiles(new FileFilter() {

                public boolean accept(File f) {
                    if(f.equals(file)) {
                        return false;
                    }
                    if(f.getName().startsWith(v.name) && f.getName().length() == v.name.length() + 8) { // '_###.vpk' = 8

                        return true;
                    }
                    return false;
                }
            });
            v.store = new File[files.length];
            v.mappings = new ByteBuffer[v.store.length];
            for(File f : files) {
                String[] split = f.getName().split("_");
                int idx = Integer.parseInt(split[split.length - 1].replaceAll(".vpk", ""));
                v.store[idx] = f;
            }
            //</editor-fold>

            v.root = v.create(v.name);
            v.root.isDirectory = true;

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

            ByteBuffer directoryInfo = DataUtils.getSlice(b, treeLength);
            v.globaldata = DataUtils.getSlice(b, dataLength);
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

            v.parseTree(directoryInfo);

        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return v;
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
                DirectoryEntry p = nodeForPath(path);
                for(;;) { // File names
                    String filename = DataUtils.readZeroString(b);
                    if(filename.length() == 0) {
                        break;
                    }
                    VPKDirectoryEntry e = readFileInfo(b,
                                                       filename + (extension != null ? ("." + extension) : ""));
                    p.add(e);
                }
            }
        }
    }

    private DirectoryEntry nodeForPath(String path) {
        DirectoryEntry parent = getRoot();
        if(!path.equals(" ")) {
            String[] components = path.split("/");
            for(String dir : components) {
                DirectoryEntry node = null;
                for(DirectoryEntry e : parent.children()) {
                    if(e.isDirectory() && e.getName().equalsIgnoreCase(dir)) {
                        node = e;
                        break;
                    }
                }
                if(node == null) {
                    VPKDirectoryEntry dirEntry = new VPKDirectoryEntry(dir);
                    dirEntry.isDirectory = true;
                    node = dirEntry;
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
    public class VPKDirectoryEntry extends DirectoryEntry {

        /**
         * A 32bit CRC of the file's data.
         */
        long CRC;

        short preloadBytes;

        short archiveIndex;

        int entryOffset;

        int entryLength;

        private ByteBuffer localdata;

        String name;

        boolean isDirectory;

        public ByteBuffer getSource() {
            if(archiveIndex == 0x7FFF) { // This archive
                return globaldata;
            }
            return getData(archiveIndex);
        }
        
        public ByteBuffer localData() {
            if(localdata == null) {
                getSource().position(entryOffset);
                localdata = DataUtils.getSlice(getSource(), entryLength);
            }
            return localdata;
        }

        VPKDirectoryEntry() {
        }

        VPKDirectoryEntry(String name) {
            this.name = name;
        }

        @Override
        public long getChecksum() {
            return CRC;
        }

        @Override
        public long calculateChecksum() {
            if(localData() == null) {
                return 0;
            }
            CRC32 crc = new CRC32();
            byte[] buf = new byte[4096];
            localData().position(0);
            while(localData().hasRemaining()) {
                int bsize = Math.min(buf.length, localData().remaining());
                localData().get(buf, 0, bsize);
                crc.update(buf, 0, bsize);
            }

            return crc.getValue();
        }

        public int getItemSize() {
            return entryLength;
        }

        public Object getAttributes() {
            return null;
        }

        public boolean isDirectory() {
            return isDirectory;
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

        public void extract(File dir) throws IOException {
            File out = new File(dir, this.name);
            if(this.isDirectory) {
                out.mkdir();
                for(DirectoryEntry e : children()) {
                    e.extract(out);
                }
            } else {
                out.createNewFile();
                InputStream is = asStream();
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[1024 * 8];
                int read;
                while((read = is.read(buf)) > -1) {
                    fos.write(buf, 0, read);
                    fos.flush();
                }
                fos.close();
            }
        }

        @Override
        public InputStream asStream() {
            return new ByteBufferInputStream(localData());
        }
    }

    private VPKDirectoryEntry root;

    public DirectoryEntry getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
