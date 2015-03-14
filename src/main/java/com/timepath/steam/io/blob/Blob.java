package com.timepath.steam.io.blob;

import com.timepath.DataUtils;
import com.timepath.DateUtils;
import com.timepath.Utils;
import com.timepath.io.utils.Savable;
import com.timepath.swing.TreeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Nodes of name \1\0\0\0 contain 'folders', \2\0\0\0 contain 'files'
 * Leaf \1\0\0\0 directories the data type used in the following \2\0\0\0 node's payload
 *
 * @author TimePath
 * @see <a>https://github.com/DHager/hl2parse/blob/master/hl2parse-binary/src/main/java/com/technofovea/hl2parse/registry
 * /RegParser.java</a>
 * @deprecated Blob files are no longer in use
 */
@Deprecated
public class Blob implements Savable {

    private static final Logger LOG = Logger.getLogger(Blob.class.getName());
    @NotNull
    public final BlobNode root;

    public Blob() {
        root = new BlobNode("Blob");
    }

    private static void parsePayload(@NotNull ByteBuffer parentbuf, @NotNull BlobNode parent, boolean rawData) {
        @NotNull ByteBuffer buf = DataUtils.getSlice(parentbuf);
        if (buf.remaining() < 2) {
            return;
        }
        short id = buf.getShort();
        @NotNull BlobNode d = new BlobNode("Payload: 0x" + Integer.toHexString(id));
        switch (id) {
            // Compressed
            /*
             * case 0x4301:
             * ByteBuffer decompressed = decompress(buf);
             * // Debug
             * int stride = 20;
             * byte[] data = new byte[stride];
             * File f = new File("binout.blob");
             * RandomAccessFile rf = null;
             * if(f != null) {
             * try {
             * f.createNewFile();
             * rf = new RandomAccessFile(f, "rw");
             * } catch(IOException ex) {
             * Logger.getLogger(Blob.class.getName()).log(Level.SEVERE, null, ex);
             * }
             * }
             * LOG.log(Level.INFO, "{0}:", decompressed.remaining());
             * for(int i = 0; i < data.length; i++) {
             * decompressed.get(data, 0, Math.min(decompressed.remaining(), stride));
             * if(rf != null) {
             * try {
             * rf.write(data);
             * } catch(IOException ex) {
             * Logger.getLogger(Blob.class.getName()).log(Level.SEVERE, null, ex);
             * }
             * }
             * LOG.info(Utils.hex(data));
             * }
             * decompressed.position(0);
             * parsePayload(decompressed, d, false);
             * break;
             */
            case 0x5001:
                int length = buf.getInt();
                int padding = buf.getInt();
                int limit = (buf.position() - 10) + length + padding; // 10 because is relative to when this section started
                //                limit = Math.min(limit, buf.position() + buf.remaining()); // workaround for decompressed
                LOG.log(Level.FINE, "limit: {0}", limit);
                buf.limit(limit);
                @NotNull ByteBuffer payload = DataUtils.getSlice(buf);
                // Payload
                while (payload.remaining() > padding) {
                    @NotNull BlobNode child = new BlobNode();
                    short descLength = payload.getShort();
                    int payloadLength = payload.getInt();
                    @NotNull ByteBuffer childDesc = DataUtils.getSlice(payload, descLength);
                    @NotNull String name = DataUtils.getText(childDesc);
                    if ("\1\0\0\0".equals(name) || "\2\0\0\0".equals(name)) {
                        childDesc.position(0);
                        child.setMeta(childDesc.getInt());
                    }
                    name = name.replaceAll("\1\0\0\0", "<Folder>");
                    name = name.replaceAll("\2\0\0\0", "<File>");
                    child.setUserObject(name);
                    @NotNull ByteBuffer childPayload = DataUtils.getSlice(payload, payloadLength);
                    if (payloadLength == 10) {
                        continue;
                    }
                    @NotNull BlobNode nextup = child;
                    if (!child.isMeta()) {
                        d.add(child);
                    } else {
                        nextup = parent;
                    }
                    if ((child.getMeta() == 1) && (payloadLength == 4)) {
                        parent.setDataType(childPayload.getInt());//parent.add(new BlobNode("Payload type: " + parent.dataType));
                    } else {
                        int dataType = parent.getDataType();
                        if (dataType != -1) {
                            switch (dataType) {
                                case 0: // Text
                                    @NotNull String str = DataUtils.getString(childPayload);
                                    @Nullable String date = DateUtils.parse(str);
                                    if (date != null) {
                                        str = "Date: " + date;
                                    }
                                    LOG.log(Level.FINE, "String: {0}", str);
                                    parent.add(new BlobNode("String: " + str));
                                    break;
                                case 1: // Dword
                                    int val = childPayload.getInt();
                                    LOG.log(Level.FINE, "DWORD: {0}", val);
                                    parent.add(new BlobNode("DWORD: " + val));
                                    break;
                                case 2: // Raw
                                    int remaining = childPayload.remaining();
                                    int max = 10;
                                    @NotNull byte[] data = new byte[Math.min(childPayload.remaining(), max)];
                                    childPayload.get(data);
                                    childPayload.position(0);
                                    @NotNull BlobNode raw = new BlobNode("Raw data: " + Utils.hex(data) +
                                            ((remaining > max) ? " ..." : "")
                                    );
                                    parent.add(raw);
                                    parsePayload(childPayload, raw, true);
                                    break;
                                default:
                                    if (!rawData) {
                                        parent.add(new BlobNode("Unhandled data type: " + dataType));
                                    }
                                    //                                LOG.log(Level.WARNING, "Unhandled data type {0}", dataType);
                                    break;
                            }
                        } else {
                            parsePayload(childPayload, nextup, false);
                        }
                    }
                }
                payload.get(new byte[padding]);
                if (buf.remaining() > 0) {
                    LOG.log(Level.INFO, "Underflow: {0}", buf.remaining());
                    return;
                }
                break;
            default:
                if (!rawData) {
                    LOG.log(Level.WARNING, "Unhandled {0}", id);
                }
                break;
        }
        TreeUtils.moveChildren(d, parent);
    }

    /**
     * Compressed header: int compressed + 20 (size of header neglecting initial
     * short) int dummy1 int decompressed int dummy2 short dummy3
     * byte[compressed] perform zlib decompression (skip the first 2 bytes) to
     * get byte[decompressed]
     *
     * @param originalBuffer compressed blob
     * @return the originalBufffer decompressed
     */
    @NotNull
    private static ByteBuffer decompress(@NotNull ByteBuffer originalBuffer) {
        LOG.log(Level.INFO,
                "Inflating a compressed binary section, initial length (including header) is {0}",
                originalBuffer.remaining());
        @NotNull Inflater inflater = new Inflater(true);
        @NotNull ByteBuffer mybuf = DataUtils.getSlice(originalBuffer);
        // Includes length of magic header etc?
        int wholeLen = mybuf.getInt(); // Includes bytes starting with itself
        int compressedLen = wholeLen - 20;
        int x1 = mybuf.getInt();
        int decompressedLen = mybuf.getInt();
        int x2 = mybuf.getInt();
        int compLevel = mybuf.getShort();
        LOG.log(Level.FINE,
                "Header claims payload compressed length is {0}, deflated length is {1}, " + "compression level {2}",
                new Object[]{compressedLen, decompressedLen, compLevel});
        if (mybuf.remaining() < compressedLen) {
            LOG.log(Level.WARNING,
                    "The buffer remainder is too small ({0}) to contain the amount of data the header specifies ({1}).",
                    new Object[]{mybuf.remaining(), compressedLen});
        }
        mybuf.limit(mybuf.position() + compressedLen);
        @NotNull byte[] compressed = new byte[mybuf.remaining()];
        mybuf.get(compressed);
        int headerSkip = 2;
        inflater.setInput(compressed, headerSkip, compressed.length - headerSkip);
        @NotNull byte[] decompressed = new byte[decompressedLen];
        LOG.fine("Beginning decompression");
        try {
            inflater.inflate(decompressed);
            LOG.fine("Decompression successful");
        } catch (DataFormatException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        @NotNull ByteBuffer newBuf = ByteBuffer.wrap(decompressed);
        newBuf.order(ByteOrder.LITTLE_ENDIAN);
        return newBuf;
    }

    @NotNull
    public BlobNode getRoot() {
        return root;
    }

    @Override
    public void readExternal(@NotNull InputStream in) {
        try {
            @NotNull byte[] buf = new byte[in.available()];
            readExternal(ByteBuffer.wrap(buf));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void readExternal(@NotNull ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        parsePayload(buf, root, false);
    }

    @Override
    public void writeExternal(OutputStream out) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
