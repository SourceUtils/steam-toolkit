package com.timepath.steam.net;

import com.timepath.DataUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Server {

    private static final Level  LEVEL_SEND = Level.FINE;
    private static final Level  LEVEL_GET  = Level.FINE;
    private static final Logger LOG        = Logger.getLogger(Server.class.getName());
    protected int             port;
    protected DatagramChannel chan;
    protected InetAddress     address;

    public Server(String hostname, int port) {
        try {
            address = InetAddress.getByName(hostname);
            this.port = port;
            InetSocketAddress sock = new InetSocketAddress(address, port);
            chan = DatagramChannel.open();
            chan.connect(sock);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public int getPort() {
        return port;
    }

    protected void send(ByteBuffer buf) throws IOException {
        LOG.log(LEVEL_SEND, "Sending {0} bytes\nPayload: {1}\nAddress: {2}", new Object[] {
                buf.limit(), DataUtils.hexDump(buf), address
        });
        chan.write(buf);
    }

    public InetAddress getAddress() {
        return address;
    }

    protected ByteBuffer get() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1392);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int bytesRead = chan.read(buf);
        if(bytesRead > 0) {
            buf.rewind();
            buf.limit(bytesRead);
            buf = buf.slice();
        }
        LOG.log(LEVEL_GET, "Receiving {0} bytes\nPayload: {1}\nAddress: {2}", new Object[] {
                buf.limit(), DataUtils.hexDump(buf), address
        });
        return buf;
    }
}
