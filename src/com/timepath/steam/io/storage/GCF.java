package com.timepath.steam.io.storage;

import com.timepath.DataUtils;
import com.timepath.EnumFlag;
import com.timepath.EnumFlags;
import com.timepath.io.RandomAccessFileWrapper;
import com.timepath.steam.io.storage.util.ExtendedVFile;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 *
 * http://wiki.singul4rity.com/steam:filestructures:gcf
 *
 * @author TimePath
 */
public class GCF extends ExtendedVFile {

    private static final Logger LOG = Logger.getLogger(GCF.class.getName());

    private final String name;

    private final RandomAccessFileWrapper raf;

    private final FileHeader header;

    private final BlockAllocationTableHeader blockAllocationTableHeader;

    private BlockAllocationTableEntry[] blocks;

    private final FileAllocationTableHeader fragMap;

    private FileAllocationTableEntry[] fragMapEntries;

    private final ManifestHeader manifestHeader;

    private GCFDirectoryEntry[] directoryEntries;

    private tagGCFDIRECTORYINFO1ENTRY[] info1Entries; // nameTable

    private tagGCFDIRECTORYINFO2ENTRY[] info2Entries; // hashTable

    private tagGCFDIRECTORYCOPYENTRY[] copyEntries; // TODO

    private tagGCFDIRECTORYLOCALENTRY[] localEntries; // TODO

    private final DirectoryMapHeader directoryMapHeader;

    private DirectoryMapEntry[] directoryMapEntries;

    private final ChecksumHeader checksumHeader;

    private final ChecksumMapHeader checksumMapHeader;

    private ChecksumMapEntry[] checksumMapEntries;

    private ChecksumEntry[] checksumEntries;

    private final DataBlockHeader dataBlockHeader;

    public GCF(File file) throws IOException {
        name = file.getName();
        raf = new RandomAccessFileWrapper(file, "r");

        header = new FileHeader();
        blockAllocationTableHeader = new BlockAllocationTableHeader();
        fragMap = new FileAllocationTableHeader();

        //<editor-fold defaultstate="collapsed" desc="Manifest">
        manifestHeader = new ManifestHeader();
        boolean skipManifest = false;
        if(skipManifest) {
            raf.skipBytes(manifestHeader.binarySize - ManifestHeader.SIZE);
        } else {
            directoryEntries = new GCFDirectoryEntry[manifestHeader.nodeCount];
            for(int i = 0; i < manifestHeader.nodeCount; i++) {
                directoryEntries[i] = new GCFDirectoryEntry(i);
            }
            byte[] ls = raf.readBytes(manifestHeader.nameSize);
            for(GCFDirectoryEntry de : directoryEntries) {
                int off = de.nameOffset;
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                while(ls[off] != 0) {
                    s.write(ls[off]);
                    off++;
                }
                de.name = new String(s.toByteArray());

                if(de.parentIndex != 0xFFFFFFFF) {
                    de.setParent(directoryEntries[de.parentIndex]);
                }
            }
            directoryEntries[0].name = this.name;

            info1Entries = new tagGCFDIRECTORYINFO1ENTRY[manifestHeader.hashTableKeyCount];
            for(int i = 0; i < manifestHeader.hashTableKeyCount; i++) {
                info1Entries[i] = new tagGCFDIRECTORYINFO1ENTRY();
            }

            info2Entries = new tagGCFDIRECTORYINFO2ENTRY[manifestHeader.nodeCount];
            for(int i = 0; i < manifestHeader.nodeCount; i++) {
                info2Entries[i] = new tagGCFDIRECTORYINFO2ENTRY();
            }

            copyEntries = new tagGCFDIRECTORYCOPYENTRY[manifestHeader.minimumFootprintCount];
            for(int i = 0; i < manifestHeader.minimumFootprintCount; i++) {
                tagGCFDIRECTORYCOPYENTRY f = new tagGCFDIRECTORYCOPYENTRY();
                f.DirectoryIndex = raf.readULEInt();

                copyEntries[i] = f;
            }

            localEntries = new tagGCFDIRECTORYLOCALENTRY[manifestHeader.userConfigCount];
            for(int i = 0; i < manifestHeader.userConfigCount; i++) {
                tagGCFDIRECTORYLOCALENTRY f = new tagGCFDIRECTORYLOCALENTRY();
                f.DirectoryIndex = raf.readULEInt();

                localEntries[i] = f;
            }
        }

        //</editor-fold>
        directoryMapHeader = new GCF.DirectoryMapHeader();

        checksumHeader = new GCF.ChecksumHeader();

        checksumMapHeader = new GCF.ChecksumMapHeader();

        dataBlockHeader = new GCF.DataBlockHeader();
    }

    @Override
    public Object getAttributes() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public ExtendedVFile getRoot() {
        return directoryEntries[0];
    }

    @Override
    public boolean isComplete() {
        return true;
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
        return name;
    }

    //<editor-fold defaultstate="collapsed" desc="Core utils">
    private BlockAllocationTableEntry getBlock(int i) throws IOException {
        BlockAllocationTableEntry bae = blocks[i];
        if(bae == null) {
            raf.seek((blockAllocationTableHeader.pos + BlockAllocationTableHeader.SIZE)
                     + (i * BlockAllocationTableEntry.SIZE));
            bae = new BlockAllocationTableEntry();
            blocks[i] = bae;
        }
        return bae;
    }

    private FileAllocationTableEntry getEntry(int i) throws IOException {
        FileAllocationTableEntry fae = fragMapEntries[i];
        if(fae == null) {
            raf.seek(
                fragMap.pos + FileAllocationTableHeader.SIZE + (i * FileAllocationTableEntry.SIZE));
            return (fragMapEntries[i] = new FileAllocationTableEntry());
        }
        return fae;
    }

    private DirectoryMapEntry directoryMapEntries(int i) {
        DirectoryMapEntry dme = directoryMapEntries[i];
        if(dme == null) {
            try {
                raf.seek(
                    directoryMapHeader.pos + DirectoryMapHeader.SIZE + (i * DirectoryMapEntry.SIZE));
                return (directoryMapEntries[i] = new DirectoryMapEntry());
            } catch(IOException ex) {
                Logger.getLogger(GCF.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return dme;
    }

    private ChecksumMapEntry checksumMapEntries(int i) throws IOException {
        ChecksumMapEntry cme = checksumMapEntries[i];
        if(cme == null) {
            raf.seek(directoryMapHeader.pos + ChecksumMapHeader.SIZE + (i * ChecksumMapEntry.SIZE));
            return (checksumMapEntries[i] = new ChecksumMapEntry());
        }
        return cme;
    }

    private ChecksumEntry checksumEntries(int i) throws IOException {
        ChecksumEntry ce = checksumEntries[i];
        if(ce == null) {
            raf.seek(
                directoryMapHeader.pos + ChecksumMapHeader.SIZE + (checksumMapEntries.length
                                                                   * ChecksumMapEntry.SIZE) + (i
                                                                                               * ChecksumEntry.SIZE));
            return (checksumEntries[i] = new ChecksumEntry());
        }
        return ce;
    }

    private byte[] readData(BlockAllocationTableEntry block, int dataIdx) throws IOException {
        long pos = (dataBlockHeader.firstBlockOffset + (dataIdx * dataBlockHeader.blockSize));
        raf.seek(pos);
        byte[] buf = new byte[dataBlockHeader.blockSize];
        if(block.fileDataOffset != 0) {
            LOG.log(Level.INFO, "off = {0}", block.fileDataOffset);
        }
        raf.read(buf);
        return buf;
    }
    //</editor-fold>

    private class FileHeader {

        /**
         * 11 * 4
         */
        private static final int SIZE = 44;

        private final long pos;

        /**
         * Always 0x00000001
         * Probably the version number for the structure
         */
        private final int headerVersion;

        /**
         * Always 0x00000001 for GCF, 0x00000002 for NCF
         */
        private final int cacheType;

        /**
         * Container version
         */
        private final int formatVersion;

        /**
         * ID of the cache
         * Can be found in the CDR section of ClientRegistry.blob
         * TF2 is 440
         */
        private final int applicationID;

        /**
         * Current revision
         */
        private final int applicationVersion;

        /**
         * Unsure
         */
        private final int isMounted;

        /**
         * Padding?
         */
        private final int dummy0;

        /**
         * Total size of GCF file in bytes
         */
        private final int fileSize;

        /**
         * Size of each data cluster in bytes
         */
        private final int clusterSize;

        /**
         * Number of data blocks
         */
        private final int clusterCount;

        /**
         * 'special' sum of all previous fields
         */
        private final int checksum;

        private FileHeader() throws IOException {
            pos = raf.getFilePointer();
            headerVersion = raf.readULEInt();
            cacheType = raf.readULEInt();
            formatVersion = raf.readULEInt();
            applicationID = raf.readULEInt();
            applicationVersion = raf.readULEInt();
            isMounted = raf.readULEInt();
            dummy0 = raf.readULEInt();
            fileSize = raf.readULEInt();
            clusterSize = raf.readULEInt();
            clusterCount = raf.readULEInt();
            checksum = raf.readULEInt();
        }

        @Override
        public String toString() {
            int checked = check();
            String checkState = (checksum == checked) ? "OK" : checksum + "vs" + checked;
            return "id:" + applicationID + ", ver:" + formatVersion + ", rev:" + applicationVersion
                   + ", mounted?: " + isMounted + ", size:" + fileSize + ", blockSize:"
                   + clusterSize + ", blocks:" + clusterCount + ", checksum:" + checkState;
        }

        private int check() {
            int checked = 0;
            checked += DataUtils.updateChecksumAddSpecial(headerVersion);
            checked += DataUtils.updateChecksumAddSpecial(cacheType);
            checked += DataUtils.updateChecksumAddSpecial(formatVersion);
            checked += DataUtils.updateChecksumAddSpecial(applicationID);
            checked += DataUtils.updateChecksumAddSpecial(applicationVersion);
            checked += DataUtils.updateChecksumAddSpecial(isMounted);
            checked += DataUtils.updateChecksumAddSpecial(dummy0);
            checked += DataUtils.updateChecksumAddSpecial(fileSize);
            checked += DataUtils.updateChecksumAddSpecial(clusterSize);
            checked += DataUtils.updateChecksumAddSpecial(clusterCount);
            return checked;
        }

    }

    private class BlockAllocationTableHeader {

        /**
         * 8 * 4
         */
        private static final int SIZE = 32;

        private final long pos;

        /**
         * Number of data blocks
         */
        private final int blockCount;

        /**
         * Number of data blocks that point to data
         */
        private final int blocksUsed;

        /**
         *
         */
        private final int lastBlockUsed;

        /**
         *
         */
        private final int dummy0;

        /**
         *
         */
        private final int dummy1;

        /**
         *
         */
        private final int dummy2;

        /**
         *
         */
        private final int dummy3;

        /**
         * Header checksum
         * The checksum is simply the sum total of all the preceeding DWORDs in the header
         */
        private final int checksum;

        private BlockAllocationTableHeader() throws IOException {
            pos = raf.getFilePointer();
            blockCount = raf.readULEInt();
            blocksUsed = raf.readULEInt();
            lastBlockUsed = raf.readULEInt();
            dummy0 = raf.readULEInt();
            dummy1 = raf.readULEInt();
            dummy2 = raf.readULEInt();
            dummy3 = raf.readULEInt();
            checksum = raf.readULEInt();

            blocks = new BlockAllocationTableEntry[blockCount];
            raf.skipBytes(blocks.length * BlockAllocationTableEntry.SIZE);
        }

        @Override
        public String toString() {
            int checked = check();
            String checkState = (checksum == checked) ? "OK" : checksum + " vs " + checked;
            return "blockCount:" + blockCount + ", blocksUsed:" + blocksUsed + ", check:"
                   + checkState;
        }

        private int check() {
            int checked = 0;
            checked += DataUtils.updateChecksumAdd(blockCount);
            checked += DataUtils.updateChecksumAdd(blocksUsed);
            checked += DataUtils.updateChecksumAdd(lastBlockUsed);
            checked += DataUtils.updateChecksumAdd(dummy0);
            checked += DataUtils.updateChecksumAdd(dummy1);
            checked += DataUtils.updateChecksumAdd(dummy2);
            checked += DataUtils.updateChecksumAdd(dummy3);
            return checked;
        }

    }

    private class BlockAllocationTableEntry {

        /**
         * 7 * 4
         */
        private static final int SIZE = 28;

//        private final long pos; // unneccesary information
        /**
         * Flags for the block entry
         * 0x200F0000 == Not used
         */
        private final int entryType;

        /**
         * The offset for the data contained in this block entry in the file
         */
        private final int fileDataOffset;

        /**
         * The length of the data in this block entry
         */
        private final int fileDataSize;

        /**
         * The index to the first data block of this block entry's data
         */
        private final int firstClusterIndex;

        /**
         * The next block entry in the series
         * (N/A if == BlockCount)
         */
        private final int nextBlockEntryIndex;

        /**
         * The previous block entry in the series
         * (N/A if == BlockCount)
         */
        private final int previousBlockEntryIndex;

        /**
         * The index of the block entry in the manifest
         */
        private final int manifestIndex;

        private BlockAllocationTableEntry() throws IOException {
            entryType = raf.readULEInt();
            fileDataOffset = raf.readULEInt();
            fileDataSize = raf.readULEInt();
            firstClusterIndex = raf.readULEInt();
            nextBlockEntryIndex = raf.readULEInt();
            previousBlockEntryIndex = raf.readULEInt();
            manifestIndex = raf.readULEInt();
        }

        @Override
        public String toString() {
            return "type:" + entryType + ", off:" + fileDataOffset + ", size:" + fileDataSize
                   + ", firstidx:" + firstClusterIndex + ", nextidx:" + nextBlockEntryIndex
                   + ", previdx:" + previousBlockEntryIndex + ", di:" + manifestIndex;
        }

    }

    private class FileAllocationTableHeader {

        /**
         * 4 * 4
         */
        private static final int SIZE = 16;

        private final long pos;

        /**
         * Number of data blocks
         */
        private final int clusterCount;

        /**
         * Index of 1st unused GCFFRAGMAP entry?
         */
        private final int firstUnusedEntry;

        /**
         * Defines the end of block chain terminator
         * If the value is 0, then the terminator is 0x0000FFFF; if the value is 1, then the
         * terminator is 0xFFFFFFFF
         */
        private final int isLongTerminator;

        /**
         * Header checksum
         */
        private final int checksum;

        private FileAllocationTableHeader() throws IOException {
            pos = raf.getFilePointer();
            clusterCount = raf.readULEInt();
            firstUnusedEntry = raf.readULEInt();
            isLongTerminator = raf.readULEInt();
            checksum = raf.readULEInt();

            fragMapEntries = new FileAllocationTableEntry[clusterCount];
            raf.skipBytes(fragMapEntries.length * FileAllocationTableEntry.SIZE);
        }

        @Override
        public String toString() {
            int checked = check();
            String checkState = (checksum == checked) ? "OK" : checksum + "vs" + checked;
            return "blockCount:" + clusterCount + ", firstUnusedEntry:" + firstUnusedEntry
                   + ", isLongTerminator:" + isLongTerminator + ", checksum:" + checkState;
        }

        private int check() {
            int checked = 0;
            checked += DataUtils.updateChecksumAdd(clusterCount);
            checked += DataUtils.updateChecksumAdd(firstUnusedEntry);
            checked += DataUtils.updateChecksumAdd(isLongTerminator);
            return checked;
        }

    }

    private class FileAllocationTableEntry {

        /**
         * 1 * 4
         */
        private static final int SIZE = 4;

        /**
         * The index of the next data block
         * If == FileAllocationTableHeader.isLongTerminator, there are no more clusters in the
         * file
         */
        private final int nextClusterIndex;

        private FileAllocationTableEntry() throws IOException {
            nextClusterIndex = raf.readULEInt();
        }

        @Override
        public String toString() {
            return "nextDataBlockIndex:" + nextClusterIndex;
        }

    }

    private enum ManifestHeaderBitmask {

        Build_Mode(0x1),
        Is_Purge_All(0x2),
        Is_Long_Roll(0x4),
        Depot_Key(0xFFFFFF00);

        int mask;

        ManifestHeaderBitmask(int mask) {
            this.mask = mask;
        }

        private static final ManifestHeaderBitmask[] flags = ManifestHeaderBitmask.values();

        private static ManifestHeaderBitmask get(int mask) {
            ManifestHeaderBitmask m = flags[mask];
            return m;
        }

    };

    private class ManifestHeader {

        /**
         * 14 * 4
         */
        private static final int SIZE = 56;

        private final long pos;

        private final int headerVersion;		// Always 0x00000004

        private final int applicationID;		// Cache ID.

        private final int applicationVersion;        // GCF file version.

        private final int nodeCount;          // Number of items in the directory.

        private final int fileCount;          // Number of files in the directory.

        private final int compressionBlockSize;		// Always 0x00008000

        /**
         * Inclusive of header
         */
        private final int binarySize;	// Size of lpGCFDirectoryEntries & lpGCFDirectoryNames & lpGCFDirectoryInfo1Entries & lpGCFDirectoryInfo2Entries & lpGCFDirectoryCopyEntries & lpGCFDirectoryLocalEntries in bytes.

        private final int nameSize;		// Size of the directory names in bytes.

        private final int hashTableKeyCount;         // Number of Info1 entires.

        private final int minimumFootprintCount;          // Number of files to copy.

        private final int userConfigCount;         // Number of files to keep local.
//        ManifestHeaderBitmask bitmask;

        private final int bitmask;

        private final int fingerprint;

        private final int checksum;

        private ManifestHeader() throws IOException {
            pos = raf.getFilePointer();
            headerVersion = raf.readULEInt();
            applicationID = raf.readULEInt();
            applicationVersion = raf.readULEInt();
            nodeCount = raf.readULEInt();
            fileCount = raf.readULEInt();
            compressionBlockSize = raf.readULEInt();
            binarySize = raf.readULEInt();
            nameSize = raf.readULEInt();
            hashTableKeyCount = raf.readULEInt();
            minimumFootprintCount = raf.readULEInt();
            userConfigCount = raf.readULEInt();
//            bitmask = ManifestHeaderBitmask.get(raf.readULEInt());
            bitmask = raf.readULEInt();
            fingerprint = raf.readULEInt();
            checksum = raf.readULEInt();
        }

        @Override
        public String toString() {
            int checked = check();
            String checkState = (checksum == checked) ? "OK" : DataUtils.toBinaryString(checksum)
                                                               + " vs " + DataUtils.toBinaryString(
                checked);
            return Long.toHexString(pos) + " : id:" + applicationID + ", ver:" + applicationVersion
                   + ", bitmask:0x" + Integer.toHexString(
                bitmask).toUpperCase() + ", items:" + nodeCount + ", files:" + fileCount
                   + ", dsize:" + binarySize + ", nsize:" + nameSize + ", info1:"
                   + hashTableKeyCount + ", copy:" + minimumFootprintCount + ", local:"
                   + userConfigCount + ", check:" + checkState;
        }

        private int check() {
            try {
                ByteBuffer bbh = ByteBuffer.allocate(SIZE);
                bbh.order(ByteOrder.LITTLE_ENDIAN);
                bbh.putInt(headerVersion);
                bbh.putInt(applicationID);
                bbh.putInt(applicationVersion);
                bbh.putInt(nodeCount);
                bbh.putInt(fileCount);
                bbh.putInt(compressionBlockSize);
                bbh.putInt(binarySize);
                bbh.putInt(nameSize);
                bbh.putInt(hashTableKeyCount);
                bbh.putInt(minimumFootprintCount);
                bbh.putInt(userConfigCount);
                bbh.putInt(bitmask);
                bbh.putInt(0);
                bbh.putInt(0);
                bbh.flip();
                byte[] bytes1 = bbh.array();

                raf.seek(pos + SIZE);
                ByteBuffer bb = ByteBuffer.allocate(binarySize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(bytes1);
                bb.put(raf.readBytes(binarySize - SIZE));
                bb.flip();
                byte[] bytes = bb.array();
                Checksum adler32 = new Adler32();
                adler32.update(bytes, 0, bytes.length);
                int checked = (int) (adler32.getValue() & 0xFFFFFFFF);
                return checked;
            } catch(IOException ex) {
                Logger.getLogger(GCF.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
        }

    }

    private enum DirectoryEntryAttributes implements EnumFlag {

        Unknown_4(0x8000),
        File(0x4000),
        Unknown_3(0x2000),
        Unknown_2(0x1000),
        Executable_File(0x800),
        Hidden_File(0x400),
        ReadOnly_File(0x200),
        Encrypted_File(0x100),
        Purge_File(0x80),
        Backup_Before_Overwriting(0x40),
        NoCache_File(0x20),
        Locked_File(0x8),
        Unknown_1(0x4),
        Launch_File(0x2),
        Configuration_File(0x1),
        Directory(0);

        private final int id;

        DirectoryEntryAttributes(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

    }

    private class GCFDirectoryEntry extends ExtendedVFile {

        /**
         * Offset to the directory item name from the end of the directory items
         */
        private final int nameOffset;

        private String name;

        /**
         * Size of the item. (If file, file size. If folder, num items.)
         */
        private final int itemSize;

        /**
         * Checksum index / file ID. (0xFFFFFFFF == None).
         */
        private final int checksumIndex;

        private final EnumSet<DirectoryEntryAttributes> attributes;

        /**
         * Index of the parent directory item. (0xFFFFFFFF == None).
         */
        private final int parentIndex;

        /**
         * Index of the next directory item. (0x00000000 == None).
         */
        private final int nextIndex;

        /**
         * Index of the first directory item. (0x00000000 == None).
         */
        private final int firstChildIndex;

        private final int index;

        private GCFDirectoryEntry(int index) throws IOException {
            this.index = index;
            nameOffset = raf.readULEInt();
            itemSize = raf.readULEInt();
            checksumIndex = raf.readULEInt();
            attributes = EnumFlags.decode(raf.readULEInt(), DirectoryEntryAttributes.class);
            parentIndex = raf.readULEInt();
            nextIndex = raf.readULEInt();
            firstChildIndex = raf.readULEInt();
        }

        public String getName() {
            return name;
        }

        public ExtendedVFile getRoot() {
            return GCF.this;
        }

        public boolean isDirectory() {
            return this.attributes.contains(DirectoryEntryAttributes.Directory);
        }

        public boolean isComplete() {
            return GCF.this.directoryMapEntries(index).firstBlockIndex < blocks.length
                   || this.itemSize == 0;
        }

        public InputStream stream() {
            return new InputStream() {

                private BlockAllocationTableEntry block;

                private final ByteBuffer buf = createBuffer();

                private byte[] data;

                private int dataIdx;

                private int pointer;

                @Override
                public int available() throws IOException {
                    return GCFDirectoryEntry.this.itemSize - pointer;
                }

                @Override
                public int read() throws IOException {
                    if(data == null || pointer > data.length) {
                        return -1;
                    }
                    return data[pointer++];
                }

                private ByteBuffer createBuffer() {
                    ByteBuffer b = ByteBuffer.wrap(new byte[GCFDirectoryEntry.this.itemSize]);
                    b.order(ByteOrder.LITTLE_ENDIAN);

                    int idx = GCF.this.directoryMapEntries(GCFDirectoryEntry.this.index).firstBlockIndex;
                    if(idx >= blocks.length) {
                        LOG.log(Level.WARNING, "Block out of range for item {0}. Is the size 0?",
                                GCFDirectoryEntry.this);
                        return null;
                    }
                    try {
                        block = GCF.this.getBlock(idx);
                        dataIdx = block.firstClusterIndex;
                        LOG.log(Level.FINE, "bSize: {0}", new Object[] {block.fileDataSize});
                        data = fill(b);
                    } catch(IOException ex) {
                        Logger.getLogger(GCF.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return b;
                }

                private byte[] fill(ByteBuffer buf) {
                    if(dataIdx == 0xFFFF || dataIdx == -1) {
                        return new byte[] {-1};
                    }
                    try {
                        byte[] b = GCF.this.readData(block, dataIdx);
                        if(buf.position() + b.length > buf.capacity()) {
                            buf.put(b, 0, block.fileDataSize % dataBlockHeader.blockSize);
                        } else {
                            buf.put(b);
                        }
                        dataIdx = GCF.this.getEntry(dataIdx).nextClusterIndex;
                        LOG.log(Level.INFO, "next dataIdx: {0}", dataIdx);
                        return buf.array();
                    } catch(IOException ex) {
                        Logger.getLogger(GCF.class.getName()).log(Level.SEVERE, null, ex);
                        return new byte[] {-1};
                    }
                }
            };
        }

        public long length() {
            return this.itemSize;
        }

        public Object getAttributes() {
            return this.attributes;
        }

        @Override
        public long getChecksum() {
            return -1;
        }

        @Override
        public long calculateChecksum() {
            return -1;
        }

        private int getIndex() {
            return index;
        }

    }

    private class tagGCFDIRECTORYINFO1ENTRY {

        /**
         * 1 * 4
         */
        private static final int SIZE = 4;

        private final int Dummy0;

        private tagGCFDIRECTORYINFO1ENTRY() throws IOException {
            Dummy0 = raf.readULEInt();
        }

        @Override
        public String toString() {
            return "" + Dummy0;
        }

    }

    private class tagGCFDIRECTORYINFO2ENTRY {

        /**
         * 1 * 4
         */
        private static final int SIZE = 4;

        private final int Dummy0;

        private tagGCFDIRECTORYINFO2ENTRY() throws IOException {
            Dummy0 = raf.readULEInt();
        }

        @Override
        public String toString() {
            return "" + Dummy0;
        }

    }

    private class DirectoryMapHeader {

        /**
         * 2 * 4
         */
        private static final int SIZE = 8;

        private final long pos;

        private final int headerVersion;     // Always 0x00000001

        private final int dummy0;     // Always 0x00000000

        private DirectoryMapHeader() throws IOException {
            pos = raf.getFilePointer();
            headerVersion = raf.readULEInt();
            dummy0 = raf.readULEInt();

            directoryMapEntries = new DirectoryMapEntry[manifestHeader.nodeCount];
            raf.skipBytes(directoryMapEntries.length * DirectoryMapEntry.SIZE);
        }

        @Override
        public String toString() {
            return "headerVersion:" + headerVersion + ", Dummy0:" + dummy0;
        }

    }

    private class DirectoryMapEntry {

        /**
         * 1 * 4
         */
        private static final int SIZE = 4;

        private final int firstBlockIndex;    // Index of the first data block. (N/A if == BlockCount.)

        private DirectoryMapEntry() throws IOException {
            firstBlockIndex = raf.readULEInt();
        }

    }

    private class ChecksumHeader {

        private final int headerVersion;			// Always 0x00000001

        private final int checksumSize;		// Size of LPGCFCHECKSUMHEADER & LPGCFCHECKSUMMAPHEADER & in bytes.
        // the number of bytes in the checksum section (excluding this structure and the following LatestApplicationVersion structure).

        private ChecksumHeader() throws IOException {
            headerVersion = raf.readULEInt();
            checksumSize = raf.readULEInt();
        }

        @Override
        public String toString() {
            return "headerVersion:" + headerVersion + ", ChecksumSize:" + checksumSize;
        }

    }

    private class ChecksumMapHeader {

        /**
         * 4 * 4
         */
        private static final int SIZE = 16;

        private final long pos;

        private final int formatCode;			// Always 0x14893721

        private final int dummy0;			// Always 0x00000001

        private final int itemCount;		// Number of items.

        private final int checksumCount;		// Number of checksums.

        private ChecksumMapHeader() throws IOException {
            pos = raf.getFilePointer();
            formatCode = raf.readULEInt();
            dummy0 = raf.readULEInt();
            itemCount = raf.readULEInt();
            checksumCount = raf.readULEInt();

            checksumMapEntries = new ChecksumMapEntry[itemCount];
            raf.skipBytes(checksumMapEntries.length * ChecksumMapEntry.SIZE);

            checksumEntries = new ChecksumEntry[checksumCount + 0x20];
            raf.skipBytes(checksumEntries.length * ChecksumEntry.SIZE);
        }

    }

    private class ChecksumMapEntry {

        /**
         * 2 * 4
         */
        private static final int SIZE = 8;

        private final int checksumCount;		// Number of checksums.

        private final int firstChecksumIndex;	// Index of first checksum.

        private ChecksumMapEntry() throws IOException {
            checksumCount = raf.readULEInt();
            firstChecksumIndex = raf.readULEInt();
        }

        @Override
        public String toString() {
            return "checkCount:" + checksumCount + ", first:" + firstChecksumIndex;
        }

    }

    private class ChecksumEntry {

        /**
         * 1 * 4
         */
        private static final int SIZE = 4;

        private final int checksum;				// Checksum.

        private ChecksumEntry() throws IOException {
            checksum = raf.readULEInt();
        }

        @Override
        public String toString() {
            return "check:" + checksum;
        }

    }

    private class DataBlockHeader {

        /**
         * GCF file version
         */
        private final int gcfRevision;

        /**
         * Number of data blocks
         */
        private final int blockCount;

        /**
         * Size of each data block in bytes
         */
        private final int blockSize;

        /**
         * Offset to first data block
         */
        private final int firstBlockOffset;

        /**
         * Number of data blocks that contain data
         */
        private final int blocksUsed;

        /**
         * Header checksum
         */
        private final int checksum;

        private DataBlockHeader() throws IOException {
            gcfRevision = raf.readULEInt();
            blockCount = raf.readULEInt();
            blockSize = raf.readULEInt();
            firstBlockOffset = raf.readULEInt();
            blocksUsed = raf.readULEInt();
            checksum = raf.readULEInt();
        }

        @Override
        public String toString() {
            int checked = 0;
            checked += DataUtils.updateChecksumAdd(blockCount);
            checked += DataUtils.updateChecksumAdd(blockSize);
            checked += DataUtils.updateChecksumAdd(firstBlockOffset);
            checked += DataUtils.updateChecksumAdd(blocksUsed);
            String checkState = (checksum == checked) ? "OK" : checksum + "vs" + checked;
            return "v:" + gcfRevision + ", blocks:" + blockCount + ", size:" + blockSize
                   + ", offset:0x" + Integer.toHexString(
                firstBlockOffset) + ", used:" + blocksUsed + ", check:" + checkState;
        }

        private int check() {
            int checked = 0;
            checked += DataUtils.updateChecksumAdd(blockCount);
            checked += DataUtils.updateChecksumAdd(blockSize);
            checked += DataUtils.updateChecksumAdd(firstBlockOffset);
            checked += DataUtils.updateChecksumAdd(blocksUsed);
            return checked;
        }

    }

    class tagGCFDIRECTORYCOPYENTRY {

        int DirectoryIndex;	// Index of the directory item.

    }

    class tagGCFDIRECTORYLOCALENTRY {

        int DirectoryIndex;	// Index of the directory item.

    }

}
