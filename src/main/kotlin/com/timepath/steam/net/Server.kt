package com.timepath.steam.net

import com.timepath.DataUtils
import com.timepath.Logger
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.util.logging.Level
import kotlin.properties.Delegates

public open class Server(hostname: String, port: Int) {
    public val address: InetAddress = InetAddress.getByName(hostname)
    public var port: Int = port
    protected val chan: DatagramChannel by Delegates.lazy {
        DatagramChannel.open().let {
            it.connect(InetSocketAddress(address, port))
            it
        }
    }

    throws(IOException::class)
    protected fun send(buf: ByteBuffer) {
        LOG.log(LEVEL_SEND, { "Sending ${buf.limit()} bytes\nPayload: ${DataUtils.hexDump(buf)}\nAddress: ${address}" })
        chan.write(buf)
    }

    throws(IOException::class)
    protected fun get(): ByteBuffer {
        val buf = ByteBuffer.allocate(1392).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            it
        }
        val bytesRead = chan.read(buf)
        LOG.log(LEVEL_GET, { "Receiving ${bytesRead} bytes\nPayload: ${DataUtils.hexDump(buf)}\nAddress: ${address}" })
        return buf.let {
            it.flip()
            it
        }
    }

    companion object {

        private val LEVEL_SEND = Level.FINE
        private val LEVEL_GET = Level.FINE
        private val LOG = Logger()
    }
}
