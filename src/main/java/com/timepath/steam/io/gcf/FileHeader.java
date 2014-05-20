package com.timepath.steam.io.gcf;

import com.timepath.DataUtils;
import com.timepath.io.RandomAccessFileWrapper;

import java.io.IOException;

/**
 * @author TimePath
 */
class FileHeader {

    /**
     * 11 * 4
     */
    static final long SIZE = 44;
    /**
     * ID of the cache.
     * Can be found in the CDR section of ClientRegistry.blob
     * TF2 is 440
     */
    private final int applicationID;
    /**
     * Current revision.
     */
    private final int applicationVersion;
    /**
     * Always 0x00000001 for GCF, 0x00000002 for NCF.
     */
    private final int cacheType;
    /**
     * 'special' sum of all previous fields.
     */
    private final int checksum;
    /**
     * Number of data blocks.
     */
    private final int clusterCount;
    /**
     * Size of each data cluster in bytes.
     */
    private final int clusterSize;
    /**
     * Padding.
     */
    private final int dummy0;
    /**
     * Total size of GCF file in bytes.
     */
    private final int fileSize;
    /**
     * Container version.
     */
    private final int formatVersion;
    /**
     * Always 0x00000001.
     * Probably the version number for the structure
     */
    private final int headerVersion;
    /**
     * Unsure.
     */
    private final int isMounted;

    FileHeader(RandomAccessFileWrapper raf) throws IOException {
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
        String checkState = ( checksum == checked ) ? "OK" : ( checksum + "vs" + checked );
        return "id:" + applicationID + ", ver:" + formatVersion + ", rev:" + applicationVersion +
               ", mounted?: " + isMounted + ", size:" + fileSize + ", blockSize:" + clusterSize + ", blocks:" +
               clusterCount + ", checksum:" + checkState;
    }

    int check() {
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
