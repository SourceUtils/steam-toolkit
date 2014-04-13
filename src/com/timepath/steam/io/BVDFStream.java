package com.timepath.steam.io;

import com.timepath.DateUtils;
import com.timepath.io.OrderedInputStream;
import com.timepath.steam.io.BVDFConstants.AppInfoState;
import com.timepath.steam.io.BVDFConstants.Section;
import com.timepath.steam.io.BVDFConstants.Universe;
import java.awt.Color;
import java.io.*;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BVDFStream {

    private static final Logger LOG = Logger.getLogger(BVDFStream.class.getName());

    private final OrderedInputStream is;

    private final BVDFListener listener;

    public BVDFStream(InputStream in, BVDFListener listener) throws IOException {
        is = new OrderedInputStream(in);
        is.order(ByteOrder.LITTLE_ENDIAN);
        this.listener = listener;
    }

    /**
     *
     * @throws IOException
     */
    public void read() throws IOException {
        for(;;) {
            ValueType type = ValueType.read(is);
            if(type == null) { // Parsing error
                LOG.log(Level.SEVERE, "Type: {0}, position: {1}", new Object[] {type, is.position()});
                return;
            }

            switch(type) {
                case TYPE_APPINFO:
                    //<editor-fold defaultstate="collapsed" desc="Code">
                    listener.emit("universe", Universe.getName(is.readInt()));
                    for(;;) {
                        int appID = is.readInt();
                        if(appID == 0) {
                            break;
                        }
                        listener.push(appID);
                        {
                            // Improvement: lazy load entries
                            int blockSize = is.readInt();

                            int appInfoState = is.readInt();
                            listener.emit("state", AppInfoState.getName(appInfoState));

                            long lastUpdated = is.readInt();
                            String formattedDate = DateUtils.parse(lastUpdated);
                            listener.emit("lastUpdated", formattedDate);

                            long token = is.readLong();
                            listener.emit("token", token);

                            byte[] sha = new byte[20];
                            is.readFully(sha);
                            listener.emit("sha", sha);

                            int changeNumber = is.readInt();
                            listener.emit("changeNumber", changeNumber);

                            listener.push("Sections");
                            {
                                for(;;) {
                                    int section = is.read();
                                    if(section == 0) {
                                        break;
                                    }

                                    listener.push(Section.get(section));
                                    {
                                        // read regular data recursively from here
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
                //</editor-fold>
                case TYPE_PACKAGEINFO:
                    //<editor-fold defaultstate="collapsed" desc="Code">
                    listener.emit("universe", Universe.getName(is.readInt()));
                    for(;;) {
                        int packageID = is.readInt();
                        if(packageID == -1) {
                            break;
                        }
                        listener.push(packageID);

                        byte[] sha = new byte[20];
                        is.readFully(sha);
                        listener.emit("sha", sha);

                        int changeNumber = is.readInt();
                        listener.emit("changeNumber", changeNumber);

                        read();

                        listener.pop();
                    }
                    return;
                //</editor-fold>
                case TYPE_NUMTYPES:
                    // Leave node
                    return;
            }

            String key = is.readString();
            switch(type) {
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
                    Color color = new Color(is.readInt(), true);
                    is.order(ByteOrder.LITTLE_ENDIAN);
                    listener.emit(key, color);
                    break;

                case TYPE_PTR:
                    listener.emit(key, is.readInt());
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

        ValueType(int... sig) {
            this.sig = sig;
        }

        final int[] sig;

        public static ValueType read(OrderedInputStream is) throws IOException {
            int i = is.read();
            for(ValueType s : ValueType.values()) {
                if(s.sig[0] == i) {
                    if(s.sig.length > 1) {
                        is.readFully(new byte[s.sig.length - 1]);
                    }
                    return s;
                }
            }
            return null;
        }

    }

}
