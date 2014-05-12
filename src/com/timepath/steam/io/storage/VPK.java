package com.timepath.steam.io.storage;

import com.timepath.DataUtils;
import com.timepath.StringUtils;
import com.timepath.io.ByteBufferInputStream;
import com.timepath.io.struct.StructField;
import com.timepath.steam.io.storage.util.ExtendedVFile;
import com.timepath.vfs.SimpleVFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Loads _dir.vpk files https://developer.valvesoftware.com/wiki/VPK_File_Format
 *
 * @author TimePath
 */
public class VPK extends ExtendedVFile {

    private static final int                       HEADER = 0x55AA1234;
    private static final Logger                    LOG    = Logger.getLogger(VPK.class.getName());
    /**
     * Previously loaded VPKs stored as references.
     */
    private static final Map<File, Reference<VPK>> cache  = new HashMap<>(0);
    private final ByteBuffer   globaldata;
    private final ByteBuffer[] mappings;
    private final File[]       store;
    private       boolean      multiPart;
    private       String       name;

    private VPK(final File file) throws IOException {
        LOG.log(Level.INFO, "Loading {0}", file);
        //<editor-fold defaultstate="collapsed" desc="Map extra archives">
        name = file.getName(); name = name.substring(0, name.length() - 4); // Strip '.vkp'
        if(name.endsWith("_dir")) {
            multiPart = true; name = name.substring(0, name.length() - 4); // Strip '_dir'
        } File[] parts = file.getParentFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if(f.equals(file)) {
                    return false;
                } return f.getName().startsWith(name) && ( f.getName().length() == ( name.length() + 8 ) );
            }
        }); store = new File[parts.length]; mappings = new ByteBuffer[store.length]; for(File f : parts) {
            String[] split = f.getName().split("_"); int idx = Integer.parseInt(split[split.length - 1].replaceAll(".vpk", ""));
            store[idx] = f;
        }
        //</editor-fold>
        ByteBuffer b = DataUtils.mapFile(file); int signature = b.getInt(); if(signature != HEADER) {
            throw new IOException("Not a VPK file");
        } int ver = b.getInt(); int treeLength = b.getInt(); // Unsigned length of directory slice
        int dataLength = 0; int v2 = 0; int v3 = 0; // 48 in most
        int v4 = 0; if(ver >= 2) {
            dataLength = b.getInt(); v2 = b.getInt(); v3 = b.getInt(); v4 = b.getInt();
        } ByteBuffer directoryInfo = DataUtils.getSlice(b, treeLength); globaldata = DataUtils.getSlice(b, dataLength);
        b.get(new byte[v2]); // Directory
        b.get(new byte[v3]); // Single + Directory
        b.get(new byte[v4]); // Directory
        Object[][] debug = {
                { "dataLength = ", dataLength },
                { "v2 = ", v2 },
                { "v3 = ", v3 },
                { "v4 = ", v4 },
                { "Underflow = ", b.remaining() },
        }; LOG.info(StringUtils.fromDoubleArray(debug, "Debug:")); parseTree(directoryInfo);
    }

    private void parseTree(ByteBuffer b) {
        // Extensions
        for(String ext; !( ext = DataUtils.readZeroString(b) ).isEmpty(); ) {
            if(" ".equals(ext)) { // No extension
                ext = null;
            }
            // Paths
            for(String dir; !( dir = DataUtils.readZeroString(b) ).isEmpty(); ) {
                SimpleVFile p = nodeForPath(dir);
                // File names
                for(String basename; !( basename = DataUtils.readZeroString(b) ).isEmpty(); ) {
                    VPKDirectoryEntry e = readFileInfo(b, basename + ( ( ext != null ) ? ( '.' + ext ) : "" )); p.add(e);
                }
            }
        }
    }

    private SimpleVFile nodeForPath(String path) {
        SimpleVFile node = getRoot(); if(!" ".equals(path)) {
            String[] components = path.split("/"); for(String dir : components) {
                SimpleVFile match = null; for(SimpleVFile e : node.list()) {
                    if(e.isDirectory() && e.getName().equalsIgnoreCase(dir)) {
                        match = e; break;
                    }
                } if(match == null) {
                    VPKDirectoryEntry dirEntry = new VPKDirectoryEntry(dir); dirEntry.isDirectory = true; match = dirEntry;
                    node.add(match);
                } node = match;
            }
        } return node;
    }

    private VPKDirectoryEntry readFileInfo(ByteBuffer b, String name) {
        VPKDirectoryEntry e = new VPKDirectoryEntry(name); e.crc = b.getInt(); e.preloadBytes = b.getShort();
        e.archiveIndex = b.getShort(); e.entryOffset = b.getInt(); e.entryLength = b.getInt();
        b.position(b.position() + e.preloadBytes); // TODO: load preload bytes
        short term = b.getShort(); assert term == 0xFFFF : "VPK directory reading failed"; return e;
    }

    public static VPK loadArchive(File file) {
        Reference<VPK> ref = cache.get(file); VPK cached = ( ref != null ) ? ref.get() : null; if(cached != null) {
            LOG.log(Level.INFO, "Loaded {0} from cache", file); return cached;
        } try {
            VPK v = new VPK(file); cache.put(file, new SoftReference<>(v)); return v;
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex); return null;
        }
    }

    public InputStream get(int index) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getAttributes() {
        return null;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    private ByteBuffer getData(int i) {
        try {
            if(mappings[i] == null) {
                mappings[i] = DataUtils.mapFile(store[i]);
            } return mappings[i];
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } return null;
    }

    /**
     * If a file contains preload data, the preload data immediately follows the
     * above structure. The entire size of a file is PreloadBytes + EntryLength.
     */
    private class VPKDirectoryEntry extends ExtendedVFile {

        /**
         * A 32bit CRC of the file's data.
         */
        @StructField(index = 0)
        int   crc;
        @StructField(index = 2)
        short archiveIndex;
        @StructField(index = 4)
        int   entryLength;
        @StructField(index = 3)
        int   entryOffset;
        boolean isDirectory;
        String  name;
        @StructField(index = 1)
        short preloadBytes;
        private Reference<ByteBuffer> localdata;

        VPKDirectoryEntry() {
        }

        VPKDirectoryEntry(String name) {
            this.name = name;
        }

        @Override
        public long calculateChecksum() {
            if(localData() == null) {
                return 0;
            } Checksum crc = new CRC32(); byte[] buf = new byte[4096]; localData().position(0);
            while(localData().hasRemaining()) {
                int bsize = Math.min(buf.length, localData().remaining()); localData().get(buf, 0, bsize);
                crc.update(buf, 0, bsize);
            } return crc.getValue();
        }

        @Override
        public Object getAttributes() {
            return null;
        }

        @Override
        public long getChecksum() {
            return crc;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ExtendedVFile getRoot() {
            return VPK.this;
        }

        public ByteBuffer getSource() {
            if(archiveIndex == 0x7FFF) { // This archive
                return globaldata;
            } return getData(archiveIndex);
        }

        @Override
        public boolean isComplete() {
            long theoretical = crc; long real = calculateChecksum(); return theoretical == real;
        }

        @Override
        public boolean isDirectory() {
            return isDirectory;
        }

        @Override
        public long length() {
            return entryLength;
        }

        public ByteBuffer localData() {
            ByteBuffer buf = ( localdata != null ) ? localdata.get() : null; if(buf == null) {
                ByteBuffer src = getSource(); src.position(entryOffset); buf = DataUtils.getSlice(src, entryLength);
                localdata = new SoftReference<>(buf);
            } return buf;
        }

        @Override
        public InputStream stream() {
            return new ByteBufferInputStream(localData());
        }
    }

    @Override
    public ExtendedVFile getRoot() {
        return this;
    }

    /**
     * VPK entries are complete by definition
     *
     * @return true
     */
    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public InputStream stream() {
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
