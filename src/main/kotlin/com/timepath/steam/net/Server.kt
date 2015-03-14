package com.timepath.steam.net

import com.timepath.DataUtils

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
public open class Server(hostname: String, port: Int) {
    public var port: Int = 0
        protected set
    protected var chan: DatagramChannel
    public var address: InetAddress
        protected set

    {
        $address = InetAddress.getByName(hostname)
        $port = port
        chan = DatagramChannel.open()
        chan.connect(InetSocketAddress(address, port))
    }

    throws(javaClass<IOException>())
    protected fun send(buf: ByteBuffer) {
        LOG.log(LEVEL_SEND, "Sending {0} bytes\nPayload: {1}\nAddress: {2}", array(buf.limit(), DataUtils.hexDump(buf), address))
        chan.write(buf)
    }

    throws(javaClass<IOException>())
    protected fun get(): ByteBuffer {
        var buf = ByteBuffer.allocate(1392)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        val bytesRead = chan.read(buf)
        if (bytesRead > 0) {
            buf.rewind()
            buf.limit(bytesRead)
            buf = buf.slice()
        }
        LOG.log(LEVEL_GET, "Receiving {0} bytes\nPayload: {1}\nAddress: {2}", array(buf.limit(), DataUtils.hexDump(buf), address))
        return buf
    }

    class object {

        private val LEVEL_SEND = Level.FINE
        private val LEVEL_GET = Level.FINE
        private val LOG = Logger.getLogger(javaClass<Server>().getName())
    }
}
