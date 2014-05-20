package com.timepath.steam.net;

import com.timepath.DataUtils;
import com.timepath.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 * @see <a>https://developer.valvesoftware.com/wiki/Master_Server_Query_Protocol</a>
 */
public class MasterServer extends Server {

    public static final  MasterServer SOURCE = new MasterServer("hl2master.steampowered.com", 27011);
    private static final Logger       LOG    = Logger.getLogger(MasterServer.class.getName());

    public MasterServer(String hostname, int port) {
        super(hostname, port);
    }

    public void query(Region r, com.timepath.steam.net.ServerListener l) throws IOException {
        query(r, "", l);
    }

    public void query(Region r, String filter, com.timepath.steam.net.ServerListener l) throws IOException {
        if(l == null) {
            l = com.timepath.steam.net.ServerListener.NULL;
        }
        String initialAddress = "0.0.0.0:0";
        String lastAddress = initialAddress;
        boolean looping = true;
        while(looping) {
            LOG.log(Level.FINE, "Last address: {0}", lastAddress);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0x31);
            baos.write(r.code);
            baos.write(( lastAddress + '\0' ).getBytes("UTF-8"));
            baos.write(( filter + '\0' ).getBytes("UTF-8"));
            ByteBuffer send = ByteBuffer.wrap(baos.toByteArray());
            send(send);
            ByteBuffer buf = get();
            int header = buf.getInt();
            if(header != -1) {
                LOG.log(Level.WARNING, "Invalid header {0}", header);
                break;
            }
            byte head = buf.get();
            if(head != 0x66) {
                LOG.log(Level.WARNING, "Unknown header {0}", head);
                String rec = DataUtils.hexDump(buf);
                LOG.log(Level.WARNING, "Received {0}", rec);
                l.inform(rec);
                break;
            }
            byte newline = buf.get();
            if(newline != 0x0A) {
                LOG.log(Level.WARNING, "Malformed byte {0}", newline);
                break;
            }
            int[] octet = new int[4];
            do {
                octet[0] = buf.get() & 0xFF;
                octet[1] = buf.get() & 0xFF;
                octet[2] = buf.get() & 0xFF;
                octet[3] = buf.get() & 0xFF;
                int serverPort = buf.getShort() & 0xFFFF;
                lastAddress = String.format("%d.%d.%d.%d:%d", octet[0], octet[1], octet[2], octet[3], serverPort);
                //noinspection AssignmentUsedAsCondition
                if(looping = !initialAddress.equals(lastAddress)) {
                    l.inform(lastAddress);
                }
            } while(buf.remaining() >= 6);
            if(buf.remaining() > 0) {
                byte[] under = new byte[buf.remaining()];
                if(under.length > 0) {
                    LOG.log(Level.WARNING, "{0} byte underflow: {0}", new Object[] {
                            buf.remaining(), Utils.hex(under)
                    });
                }
            }
        }
    }

    public static enum Region {
        ALL(255),
        US_EAST(0),
        US_WEST(1),
        SOUTH_AMERICA(2),
        EUROPE(3),
        ASIA(4),
        AUSTRALIA(5),
        MIDDLE_EAST(6),
        AFRICA(7);
        private final byte code;

        private Region(int code) {
            this.code = (byte) code;
        }
    }
}
