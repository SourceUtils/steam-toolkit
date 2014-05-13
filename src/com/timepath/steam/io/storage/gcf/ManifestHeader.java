package com.timepath.steam.io.storage.gcf;

import com.timepath.DataUtils;
import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * @author TimePath
 */
class ManifestHeader {

    /**
     * 14 * 4
     */
    static final int SIZE = 56;
    /**
     * Size of lpGCFDirectoryEntries & lpGCFDirectoryNames & lpGCFDirectoryInfo1Entries
     * & lpGCFDirectoryInfo2Entries & lpGCFDirectoryCopyEntries & lpGCFDirectoryLocalEntries in bytes.
     * Inclusive of header
     */
    final         int                     binarySize;
    /**
     * Number of Info1 entries.
     */
    final         int                     hashTableKeyCount;
    /**
     * Number of files to copy.
     */
    final         int                     minimumFootprintCount;
    /**
     * Size of the directory names in bytes.
     */
    final         int                     nameSize;
    /**
     * Number of items in the directory.
     */
    final         int                     nodeCount;
    /**
     * Number of files to keep local.
     */
    final         int                     userConfigCount;
    private final RandomAccessFileWrapper raf;
    /**
     * Cache ID.
     */
    private final int                     applicationID;
    /**
     * GCF file version.
     */
    private final int                     applicationVersion;
    /**
     * TODO: ManifestHeaderBitmask bitmask;
     */
    private final int                     bitmask;
    private final int                     checksum;
    /**
     * Always 0x00008000.
     */
    private final int                     compressionBlockSize;
    /**
     * Number of files in the directory.
     */
    private final int                     fileCount;
    /**
     * Always 0x00000004.
     */
    private final int                     headerVersion;
    private final long                    pos;

    ManifestHeader(RandomAccessFileWrapper raf) throws IOException {
        this.raf = raf;
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
        int fingerprint = raf.readULEInt();
        checksum = raf.readULEInt();
    }

    @Override
    public String toString() {
        int checked = check();
        String checkState = ( checksum == checked ) ? "OK" : ( DataUtils.toBinaryString(checksum) + " vs " +
                                                               DataUtils.toBinaryString(checked) );
        return MessageFormat.format(
                "{0} : id:{1}, ver:{2}, bitmask:0x{3}, items:{4}, files:{5}, dsize:{6}, nsize:{7}, info1:{8}, " +
                "copy:{9}, local:{10}, check:{11}",
                Long.toHexString(pos),
                applicationID,
                applicationVersion,
                Integer.toHexString(bitmask).toUpperCase(),
                nodeCount,
                fileCount,
                binarySize,
                nameSize,
                hashTableKeyCount,
                minimumFootprintCount,
                userConfigCount,
                checkState
                                   );
    }

    int check() {
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
            int checked = (int) adler32.getValue();
            return checked;
        } catch(IOException ex) {
            Logger.getLogger(GCF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
}
