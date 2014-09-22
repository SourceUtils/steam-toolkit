package com.timepath.steam.net;

import com.timepath.DataUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 * @see <a>https://developer.valvesoftware.com/wiki/Server_Queries</a>
 */
public class SourceServer extends Server {

    private static final Logger LOG = Logger.getLogger(SourceServer.class.getName());
    private static final byte[] HEADER = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    public SourceServer(String hostname, int port) {
        super(hostname, port);
    }

    public void getInfo(com.timepath.steam.net.ServerListener listener) throws IOException {
        if (listener == null) {
            listener = com.timepath.steam.net.ServerListener.NULL;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(HEADER);
        // A2S_INFO
        baos.write(0x54);
        baos.write(("Source Engine Query" + '\0').getBytes("UTF-8"));
        send(ByteBuffer.wrap(baos.toByteArray()));
        ByteBuffer buf = get();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int packHeader = buf.getInt();
        if (packHeader != -1) {
            LOG.log(Level.SEVERE, "Invalid packet header {0}", packHeader);
        }
        byte header = buf.get();
        if (header != 0x49) {
            LOG.log(Level.SEVERE, "Invalid header {0}", header);
        }
        byte protocol = buf.get();
        listener.inform("Protocol: " + protocol);
        String name = DataUtils.getString(buf);
        listener.inform("Name: '" + name + '\'');
        String map = DataUtils.getString(buf);
        listener.inform("Map: '" + map + '\'');
        String gamedir = DataUtils.getString(buf);
        listener.inform("Gamedir: '" + gamedir + '\'');
        String game = DataUtils.getString(buf);
        listener.inform("Game: '" + game + '\'');
        short appID = buf.getShort();
        listener.inform("AppID: '" + appID + '\'');
        byte playerCount = buf.get();
        listener.inform("Players: '" + playerCount + '\'');
        byte playerCountMax = buf.get();
        listener.inform("Capacity: '" + playerCountMax + '\'');
        byte botCount = buf.get();
        listener.inform("Bots: '" + botCount + '\'');
        ServerType type = ServerType.valueFor(buf.get());
        listener.inform("Type: '" + type + '\'');
        Environment env = Environment.valueFor(buf.get());
        listener.inform("Environment: '" + env + '\'');
        boolean visibility = buf.get() == 0;
        listener.inform("Visible: '" + visibility + '\'');
        boolean secure = buf.get() == 1;
        listener.inform("VAC: '" + secure + '\'');
        String version = DataUtils.getString(buf);
        listener.inform("Version: '" + version + '\'');
        byte edf = buf.get();
        boolean edfPort = (edf & 0b10000000) != 0;
        boolean edfSTV = (edf & 0b1000000) != 0;
        boolean edfTags = (edf & 0b100000) != 0;
        boolean edfSteamID = (edf & 0b10000) != 0;
        boolean edfGameID = (edf & 0b1) != 0;
        if (edfPort) {
            short portLocal = buf.getShort();
            listener.inform("Port: '" + portLocal + '\'');
        }
        if (edfSteamID) { // TODO: check
            //            ByteBuffer d = buf.duplicate();
            //            d.limit(buf.position() + 8);
            //            LOG.info(DataUtils.hexDump(d.slice()));
            BigInteger sid = BigInteger.valueOf(buf.getLong());
            if (sid.compareTo(BigInteger.ZERO) < 0) {
                sid = sid.add(BigInteger.ONE.shiftLeft(64));
            }
            listener.inform("SteamID: '" + sid + '\'');
        }
        if (edfSTV) {
            short stvPort = buf.getShort();
            listener.inform("STV Port: '" + stvPort + '\'');
            String stvName = DataUtils.getString(buf);
            listener.inform("STV Name: '" + stvName + '\'');
        }
        if (edfTags) {
            String tags = DataUtils.getString(buf);
            listener.inform("Tags: '" + tags + '\'');
        }
        if (edfGameID) {
            BigInteger gid = BigInteger.valueOf(buf.getLong());
            if (gid.compareTo(BigInteger.ZERO) < 0) {
                gid = gid.add(BigInteger.ONE.shiftLeft(64));
            }
            listener.inform("GameID: '" + gid + '\'');
        }
    }

    public void getRules(com.timepath.steam.net.ServerListener l) throws IOException {
        if (l == null) {
            l = com.timepath.steam.net.ServerListener.NULL;
        }
        // Get a challenge key
        ByteArrayOutputStream challengeOut = new ByteArrayOutputStream();
        challengeOut.write(HEADER);
        challengeOut.write(0x56);
        challengeOut.write(HEADER);
        ByteBuffer challengeSend = ByteBuffer.wrap(challengeOut.toByteArray());
        send(challengeSend);
        ByteBuffer challengeGet = get();
        challengeGet.order(ByteOrder.LITTLE_ENDIAN);
        int challengepackHeader = challengeGet.getInt();
        if (challengepackHeader != -1) {
            LOG.log(Level.SEVERE, "Invalid packet header {0}", challengepackHeader);
        }
        byte challengeheader = challengeGet.get();
        if (challengeheader != 0x41) {
            LOG.log(Level.SEVERE, "Invalid header {0}", challengeheader);
        }
        byte[] challengeKey = new byte[4];
        challengeGet.get(challengeKey);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(HEADER);
        baos.write(0x56);
        baos.write(challengeKey);
        ByteBuffer send = ByteBuffer.wrap(baos.toByteArray());
        ByteBuffer ruleBuf = ByteBuffer.allocate(4000);
        ruleBuf.order(ByteOrder.LITTLE_ENDIAN);
        int ruleCount = 0;
        while (true) {
            send.rewind();
            send(send);
            ByteBuffer buf = get();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int packHeader = buf.getInt();
            if (packHeader != -2) {
                LOG.log(Level.SEVERE, "Invalid packet header {0}", packHeader);
            }
            int reqID = buf.getInt();
            int fragments = buf.get();
            int id = buf.get() + 1; // zero-indexed
            int payloadLength = buf.getShort();
            if (id == 1) {
                int pack2Header = buf.getInt();
                if (pack2Header != -1) {
                    LOG.log(Level.SEVERE, "Invalid packHeader {0}", pack2Header);
                }
                byte header = buf.get();
                if (header != 0x45) {
                    LOG.log(Level.SEVERE, "Invalid header {0}", header);
                }
                ruleCount = buf.getShort();
            }
            LOG.log(Level.FINE, "{0} / {1}", new Object[]{id, fragments});
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            ruleBuf.put(data);
            if (id == fragments) {
                break;
            }
        }
        ruleBuf.flip();
        LOG.log(Level.FINE, "Rules: {0}", ruleCount);
        LOG.log(Level.FINE, "Remaining: {0}", ruleBuf.remaining());
        for (int ruleIndex = 1; ruleIndex < (ruleCount + 1); ruleIndex++) {
            if (ruleBuf.remaining() == 0) {
                break;
            }
            String key = DataUtils.getString(ruleBuf);
            String value = DataUtils.getString(ruleBuf);
            l.inform("[" + ruleIndex + '/' + ruleCount + "] " + '\'' + key + "' = '" + value + '\'');
        }
        LOG.log(Level.FINE, "Underflow: {0}", ruleBuf.remaining());
    }

    private enum ServerType {
        DEDICATED('d'),
        LISTEN('l'),
        SOURCE_TV('p');
        char code;

        ServerType(char code) {
            this.code = code;
        }

        public static ServerType valueFor(byte b) {
            for (ServerType t : ServerType.values()) {
                if (t.code == b) {
                    return t;
                }
            }
            return null;
        }
    }

    private enum Environment {
        WINDOWS('w'),
        LINUX('l');
        char code;

        Environment(char code) {
            this.code = code;
        }

        public static Environment valueFor(byte b) {
            for (Environment t : Environment.values()) {
                if (t.code == b) {
                    return t;
                }
            }
            return null;
        }
    }
}
