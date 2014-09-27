package com.timepath.steam.io.bvdf;

import com.timepath.DateUtils;
import com.timepath.io.OrderedInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tokenizes binary BDF files and notifies a listener
 *
 * @author TimePath
 * @see <a>https://github.com/harvimt/steam_launcher/blob/master/binvdf.py</a>
 * @see <a>https://github.com/barneygale/bvdf/blob/master/bvdf.py</a>
 * @see <a>https://github.com/DHager/hl2parse</a>
 * @see <a>http://cs.rin.ru/forum/viewtopic.php?f=20&t=61506&hilit=appinfo</a>
 * @see <a>http://cs.rin.ru/forum/viewtopic.php?f=20&t=62438&hilit=packageinfo</a>
 * @see <a>http://media.steampowered.com/steamcommunity/public/images/apps/[appID]/[sha].[ext]</a>
 * @see <a>http://cdr.xpaw.ru/app/5/#section_info</a>
 * @see <a>http://hlssmod.net/he_code/public/tier1/KeyValues.h</a>
 * @see <a>http://hpmod.googlecode.com/svn/trunk/tier1/KeyValues.cpp</a>
 */
public class BVDFStream {

    private static final Logger LOG = Logger.getLogger(BVDFStream.class.getName());
    @NotNull
    private final OrderedInputStream is;
    private final BVDFListener listener;

    public BVDFStream(@NotNull InputStream in, BVDFListener listener) throws IOException {
        is = new OrderedInputStream(in);
        is.order(ByteOrder.LITTLE_ENDIAN);
        this.listener = listener;
    }

    /**
     * @throws IOException
     */
    public void read() throws IOException {
        while (true) {
            @Nullable ValueType type = ValueType.read(is);
            if (type == null) { // Parsing error
                LOG.log(Level.SEVERE, "Type: {0}, position: {1}", new Object[]{type, is.position()}); // TODO: pushback header
                return;
            }
            switch (type) {
                case TYPE_APPINFO:
                    listener.emit("universe", BVDFConstants.Universe.getName(is.readInt()));
                    while (true) {
                        int appID = is.readInt();
                        if (appID == 0) {
                            break;
                        }
                        listener.push(appID);
                        {
                            // Improvement: lazy load entries
                            int blockSize = is.readInt();
                            int appInfoState = is.readInt();
                            listener.emit("state", BVDFConstants.AppInfoState.getName(appInfoState));
                            long lastUpdated = is.readInt();
                            String formattedDate = DateUtils.parse(lastUpdated);
                            listener.emit("lastUpdated", formattedDate);
                            long token = is.readLong();
                            listener.emit("token", token);
                            @NotNull byte[] sha = new byte[20];
                            is.readFully(sha);
                            listener.emit("sha", sha);
                            int changeNumber = is.readInt();
                            listener.emit("changeNumber", changeNumber);
                            listener.push("Sections");
                            {
                                while (true) {
                                    int section = is.read();
                                    if (section == 0) {
                                        break;
                                    }
                                    listener.push(BVDFConstants.Section.get(section));
                                    {
                                        // Read regular data recursively from here
                                        read();
                                    }
                                    listener.pop();
                                }
                            }
                            listener.pop();
                        }
                        listener.pop();
                    }
                    return;
                case TYPE_PACKAGEINFO:
                    listener.emit("universe", BVDFConstants.Universe.getName(is.readInt()));
                    while (true) {
                        int packageID = is.readInt();
                        if (packageID == -1) {
                            break;
                        }
                        listener.push(packageID);
                        @NotNull byte[] sha = new byte[20];
                        is.readFully(sha);
                        listener.emit("sha", sha);
                        int changeNumber = is.readInt();
                        listener.emit("changeNumber", changeNumber);
                        read();
                        listener.pop();
                    }
                    return;
                case TYPE_NUMTYPES:
                    // Leave node
                    return;
            }
            @NotNull String key = is.readString();
            switch (type) {
                case TYPE_NONE:
                    listener.push(key);
                    read();
                    listener.pop();
                    break;
                case TYPE_STRING:
                    listener.emit(key, is.readString());
                    break;
                case TYPE_WSTRING:
                    LOG.log(Level.WARNING, "Detected {0}, this should never happen", type);
                    listener.emit(key, is.readString());
                    break;
                case TYPE_INT:
                case TYPE_PTR:
                    listener.emit(key, is.readInt());
                    break;
                case TYPE_UINT64:
                    listener.emit(key, is.readLong());
                    break;
                case TYPE_FLOAT:
                    listener.emit(key, is.readFloat());
                    break;
                case TYPE_COLOR:
                    is.order(ByteOrder.BIG_ENDIAN);
                    @NotNull Color color = new Color(is.readInt(), true);
                    is.order(ByteOrder.LITTLE_ENDIAN);
                    listener.emit(key, color);
                    break;
                default:
                    LOG.log(Level.SEVERE, "Unhandled data type {0}", type);
                    break;
            }
        }
    }

    private enum ValueType {
        TYPE_NONE(0), TYPE_STRING(1), TYPE_INT(2), TYPE_FLOAT(3), TYPE_PTR(4), TYPE_WSTRING(5), TYPE_COLOR(6),
        TYPE_UINT64(7), TYPE_NUMTYPES(8),
        TYPE_APPINFO(0x26, 0x44, 0x56, 0x07),
        TYPE_PACKAGEINFO(0x27, 0x55, 0x56, 0x06);
        final int[] sig;

        ValueType(int... sig) {
            this.sig = sig;
        }

        @Nullable
        public static ValueType read(@NotNull OrderedInputStream is) throws IOException {
            int i = is.read();
            for (@NotNull ValueType type : ValueType.values()) {
                if (type.sig[0] == i) {
                    if (type.sig.length > 1) {
                        is.readFully(new byte[type.sig.length - 1]);
                    }
                    return type;
                }
            }
            return null;
        }
    }
}
