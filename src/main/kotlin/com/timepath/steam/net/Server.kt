package com.timepath.steam.net

import com.timepath.DataUtils

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.Delegates
import java.net.InetAddress

/**
 * @author TimePath
 */
public open class Server(hostname: String, port: Int) {
    public val address: InetAddress = InetAddress.getByName(hostname)
    public var port: Int = port
    protected val chan: DatagramChannel by Delegates.lazy {
        DatagramChannel.open().let {
            it.connect(InetSocketAddress(address, port))
            it
        }
    }

    throws(javaClass<IOException>())
    protected fun send(buf: ByteBuffer) {
        LOG.log(LEVEL_SEND, "Sending {0} bytes\nPayload: {1}\nAddress: {2}", array(buf.limit(), DataUtils.hexDump(buf), address))
        chan.write(buf)
    }

    throws(javaClass<IOException>())
    protected fun get(): ByteBuffer {
        val buf = ByteBuffer.allocate(1392).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            it
        }
        val bytesRead = chan.read(buf)
        LOG.log(LEVEL_GET, "Receiving {0} bytes\nPayload: {1}\nAddress: {2}", array(bytesRead, DataUtils.hexDump(buf), address))
        return buf.let {
            it.flip()
            it
        }
    }

    companion object {

        private val LEVEL_SEND = Level.FINE
        private val LEVEL_GET = Level.FINE
        private val LOG = Logger.getLogger(javaClass<Server>().getName())
    }
}
