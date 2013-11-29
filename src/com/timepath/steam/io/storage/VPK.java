package com.timepath.steam.io.storage;

import com.timepath.DataUtils;
import com.timepath.StringUtils;
import com.timepath.io.ByteBufferInputStream;
import com.timepath.steam.io.storage.util.ExtendedVFile;
import com.timepath.vfs.SimpleVFile;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * Loads _dir.vpk files https://developer.valvesoftware.com/wiki/VPK_File_Format
 *
 * @author TimePath
 */
public class VPK extends ExtendedVFile {

    private static int HEADER = 0x55AA1234;

    private static final Logger LOG = Logger.getLogger(VPK.class.getName());

    public static VPK loadArchive(final File file) {
        return loadArchive(new VPK(), file);
    }

    public static VPK loadArchive(final VPK v, final File file) {
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
                return f.getName().startsWith(v.name) && f.getName().length() == v.name.length() + 8;
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

        try {
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
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }

        return v;
    }

    private ByteBuffer globaldata;

    private ByteBuffer[] mappings;

    private boolean multiPart;

    private String name;

    private File[] store;

    public VPKDirectoryEntry create(String name) {
        return new VPKDirectoryEntry(name);
    }

    public InputStream get(int index) {
        return null;
    }

    @Override
    public ExtendedVFile getRoot() {
        return this;
    }

    @Override
    public Object getAttributes() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isComplete() {
        return true; // They have to be
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public InputStream stream() {
        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }

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

    private SimpleVFile nodeForPath(String path) {
        SimpleVFile node = getRoot();
        if(!path.equals(" ")) {
            String[] components = path.split("/");
            for(String dir : components) {
                SimpleVFile match = null;
                for(SimpleVFile e : node.list()) {
                    if(e.isDirectory() && e.getName().equalsIgnoreCase(dir)) {
                        match = e;
                        break;
                    }
                }
                if(match == null) {
                    VPKDirectoryEntry dirEntry = new VPKDirectoryEntry(dir);
                    dirEntry.isDirectory = true;
                    match = dirEntry;
                    node.add(match);
                }
                node = match;
            }
        }
        return node;
    }

    private void parseTree(ByteBuffer b) {
        for(;;) { // Extensions
            String ext = DataUtils.readZeroString(b);
            if(ext.length() == 0) { // End of data
                break;
            }
            if(ext.equals(" ")) { // No extension
                ext = null;
            }
            for(;;) { // Paths
                String path = DataUtils.readZeroString(b);
                if(path.length() == 0) {
                    break;
                }
                SimpleVFile p = nodeForPath(path);
                for(;;) { // File names
                    String name = DataUtils.readZeroString(b);
                    if(name.length() == 0) {
                        break;
                    }
                    VPKDirectoryEntry e = readFileInfo(b, name + (ext != null ? ("." + ext) : ""));
                    p.add(e);
                }
            }
        }
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

    /**
     * If a file contains preload data, the preload data immediately follows the
     * above structure. The entire size of a file is PreloadBytes + EntryLength.
     */
    public class VPKDirectoryEntry extends ExtendedVFile {

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

        @Override
        public long length() {
            return entryLength;
        }

        public Object getAttributes() {
            return null;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public ExtendedVFile getRoot() {
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

        @Override
        public InputStream stream() {
            return new ByteBufferInputStream(localData());
        }

    }

}
