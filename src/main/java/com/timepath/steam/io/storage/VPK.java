package com.timepath.steam.io.storage;

import com.timepath.DataUtils;
import com.timepath.StringUtils;
import com.timepath.io.ByteBufferInputStream;
import com.timepath.io.struct.StructField;
import com.timepath.steam.io.storage.Files.FileHandler;
import com.timepath.steam.io.util.ExtendedVFile;
import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Loads _dir.vpk files
 *
 * @author TimePath
 * @see <a>https://developer.valvesoftware.com/wiki/VPK_File_Format</a>
 */
public class VPK extends ExtendedVFile {

    private static final int HEADER = 0x55AA1234;
    private static final Logger LOG = Logger.getLogger(VPK.class.getName());
    /**
     * Previously loaded VPKs stored as references.
     */
    private static final Map<File, Reference<VPK>> REFERENCE_MAP = new HashMap<>(0);
    private final ByteBuffer globaldata;
    @NotNull
    private final ByteBuffer[] mappings;
    @NotNull
    private final File[] store;
    private String name;

    static {
        Files.registerHandler(new FileHandler() {
            @Nullable
            @Override
            public Collection<? extends SimpleVFile> handle(@NotNull final File file) throws IOException {
                if (!file.getName().endsWith("_dir.vpk")) return null;
                return VPK.loadArchive(file).list();
            }
        });
    }

    private VPK(@NotNull final File file) throws IOException {
        LOG.log(Level.INFO, "Loading {0}", file);
        // Map extra archives
        name = file.getName();
        name = name.substring(0, name.length() - 4); // Strip '.vkp'
        if (name.endsWith("_dir")) {
            name = name.substring(0, name.length() - 4); // Strip '_dir'
        }
        File[] parts = file.getParentFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(@NotNull File pathname) {
                return !pathname.equals(file) && pathname.getName().startsWith(name) &&
                        (pathname.getName().length() == (name.length() + 8));
            }
        });
        store = new File[parts.length];
        mappings = new ByteBuffer[store.length];
        for (@NotNull File part : parts) {
            @NotNull String[] split = part.getName().split("_");
            int idx = Integer.parseInt(split[split.length - 1].replaceAll(".vpk", ""));
            store[idx] = part;
        }
        ByteBuffer buffer = DataUtils.mapFile(file);
        int signature = buffer.getInt();
        if (signature != HEADER) {
            throw new IOException("Not a VPK file");
        }
        int ver = buffer.getInt();
        int treeLength = buffer.getInt(); // Unsigned length of directory slice
        int dataLength = 0;
        int v2 = 0;
        int v3 = 0; // 48 in most
        int v4 = 0;
        if (ver >= 2) {
            dataLength = buffer.getInt();
            v2 = buffer.getInt();
            v3 = buffer.getInt();
            v4 = buffer.getInt();
        }
        ByteBuffer directoryInfo = DataUtils.getSlice(buffer, treeLength);
        globaldata = DataUtils.getSlice(buffer, dataLength);
        buffer.get(new byte[v2]); // Directory
        buffer.get(new byte[v3]); // Single + Directory
        buffer.get(new byte[v4]); // Directory
        @NotNull Object[][] debug = {
                {"dataLength = ", dataLength},
                {"v2 = ", v2},
                {"v3 = ", v3},
                {"v4 = ", v4},
                {"Underflow = ", buffer.remaining()},
        };
        LOG.info(StringUtils.fromDoubleArray(debug, "Debug:"));
        parseTree(directoryInfo);
    }

    @Nullable
    public static VPK loadArchive(@NotNull File file) {
        Reference<VPK> ref = REFERENCE_MAP.get(file);
        @Nullable VPK cached = (ref != null) ? ref.get() : null;
        if (cached != null) {
            LOG.log(Level.INFO, "Loaded {0} from cache", file);
            return cached;
        }
        try {
            @NotNull VPK v = new VPK(file);
            REFERENCE_MAP.put(file, new SoftReference<>(v));
            return v;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private void parseTree(@NotNull ByteBuffer buffer) {
        // Extensions
        for (String ext; !(ext = DataUtils.readZeroString(buffer)).isEmpty(); ) {
            ext = " ".equals(ext) ? "" : '.' + ext;
            // Paths
            for (String dir; !(dir = DataUtils.readZeroString(buffer)).isEmpty(); ) {
                @Nullable SimpleVFile p = nodeForPath(dir);
                // File names
                for (String basename; !(basename = DataUtils.readZeroString(buffer)).isEmpty(); ) {
                    @NotNull VPKDirectoryEntry e = readFileInfo(buffer, basename + ext);
                    p.add(e);
                }
            }
        }
    }

    @Nullable
    private SimpleVFile nodeForPath(@NotNull String path) {
        @Nullable SimpleVFile node = getRoot();
        if (" ".equals(path)) {
            return node;
        }
        @NotNull String[] components = path.replace('\\', '/').split("/");
        for (String dir : components) {
            @Nullable SimpleVFile match = null;
            for (@NotNull SimpleVFile e : node.list()) {
                if (e.isDirectory() && e.getName().equalsIgnoreCase(dir)) {
                    match = e;
                    break;
                }
            }
            if (match == null) {
                @NotNull VPKDirectoryEntry dirEntry = new VPKDirectoryEntry(dir);
                dirEntry.isDirectory = true;
                match = dirEntry;
                node.add(match);
            }
            node = match;
        }
        return node;
    }

    @NotNull
    private VPKDirectoryEntry readFileInfo(@NotNull ByteBuffer buffer, String name) {
        @NotNull VPKDirectoryEntry e = new VPKDirectoryEntry(name);
        e.crc = buffer.getInt();
        e.preloadBytes = buffer.getShort();
        e.archiveIndex = buffer.getShort();
        e.entryOffset = buffer.getInt();
        e.entryLength = buffer.getInt();
        buffer.position(buffer.position() + e.preloadBytes); // TODO: load preload bytes
        int term = buffer.getShort() & 0xFFFF;
        assert term == 0xFFFF : "VPK directory reading failed";
        return e;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Nullable
    private ByteBuffer getData(int i) {
        try {
            if (mappings[i] == null) {
                mappings[i] = DataUtils.mapFile(store[i]);
            }
            return mappings[i];
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getAttributes() {
        return null;
    }

    @NotNull
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

    @Nullable
    @Override
    public InputStream openStream() {
        return null;
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
        int crc;
        @StructField(index = 2)
        short archiveIndex;
        @StructField(index = 4)
        int entryLength;
        @StructField(index = 3)
        int entryOffset;
        boolean isDirectory;
        String name;
        @StructField(index = 1)
        short preloadBytes;
        @Nullable
        private Reference<ByteBuffer> localdata;

        VPKDirectoryEntry(String name) {
            this.name = name;
        }

        @Override
        public long calculateChecksum() {
            if (localData() == null) {
                return 0;
            }
            @NotNull Checksum checksum = new CRC32();
            localData().position(0);
            @NotNull byte[] buf = new byte[4096];
            while (localData().hasRemaining()) {
                int bsize = Math.min(buf.length, localData().remaining());
                localData().get(buf, 0, bsize);
                checksum.update(buf, 0, bsize);
            }
            return checksum.getValue();
        }

        @Nullable
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

        @NotNull
        @Override
        public ExtendedVFile getRoot() {
            return VPK.this;
        }

        @Nullable
        public ByteBuffer getSource() {
            if (archiveIndex == 0x7FFF) { // This archive
                return globaldata;
            }
            return getData(archiveIndex);
        }

        @Override
        public boolean isComplete() {
            long theoretical = crc;
            long real = calculateChecksum();
            return theoretical == real;
        }

        @Override
        public boolean isDirectory() {
            return isDirectory;
        }

        @Override
        public long length() {
            return entryLength;
        }

        @Nullable
        public ByteBuffer localData() {
            @Nullable ByteBuffer buf = (localdata != null) ? localdata.get() : null;
            if (buf == null) {
                @Nullable ByteBuffer src = getSource();
                src.position(entryOffset);
                buf = DataUtils.getSlice(src, entryLength);
                localdata = new SoftReference<>(buf);
            }
            return buf;
        }

        @Nullable
        @Override
        public InputStream openStream() {
            return new ByteBufferInputStream(localData());
        }
    }
}
