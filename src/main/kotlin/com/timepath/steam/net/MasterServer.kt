package com.timepath.steam.net

import com.timepath.DataUtils
import com.timepath.Logger
import com.timepath.Utils

import java.io.IOException
import java.nio.ByteBuffer
import java.util.logging.Level

/**
 * @see <a>https://developer.valvesoftware.com/wiki/Master_Server_Query_Protocol</a>
 */
public class MasterServer(hostname: String, port: Int) : Server(hostname, port) {

    throws(IOException::class)
    public fun query(region: Region, filter: String = "", listener: ServerListener = ServerListener.NULL) {
        val initialAddress = "0.0.0.0:0"
        val filterBytes = filter.toByteArray()
        val write = ByteBuffer.allocate(0
                + /* header */ 1
                + /* region */ 4
                + /* ip - arbitrary */ 30
                + /* filter */ (filterBytes.size() + 1)
        )
        var address = initialAddress
        outer@
        while (true) {
            LOG.log(Level.FINE, { "Last address: ${address}" })
            with(write) {
                reset()
                put(0x31)
                putInt(region.code.toInt())
                put(address.toByteArray())
                put(0)
                put(filterBytes)
                put(0)
                flip()
            }
            send(write)
            val recv = get()
            val header = recv.getInt()
            if (header != -1) {
                LOG.log(Level.WARNING, { "Invalid header ${header}" })
                break
            }
            val head = recv.get()
            if (head.toInt() != 0x66) {
                LOG.log(Level.WARNING, { "Unknown header ${head}" })
                val rec = DataUtils.hexDump(recv)
                LOG.log(Level.WARNING, { "Received ${rec}" })
                listener.inform(rec)
                break
            }
            val newline = recv.get()
            if (newline.toInt() != 0x0A) {
                LOG.log(Level.WARNING, { "Malformed byte ${newline}" })
                break
            }
            do {
                val o0 = recv.get().toInt() and 0xFF
                val o1 = recv.get().toInt() and 0xFF
                val o2 = recv.get().toInt() and 0xFF
                val o3 = recv.get().toInt() and 0xFF
                val port = recv.getShort().toInt() and 0xFFFF
                address = "$o0.$o1.$o2.$o3:$port"
                if (address == initialAddress) {
                    break@outer
                }
                listener.inform(address)
            } while (recv.remaining() >= 6)
            if (recv.capacity() - recv.position() > 0) {
                val under = recv.slice()
                LOG.log(Level.WARNING, { "${recv.remaining()} byte underflow: ${Utils.hex(*under.array())}" })
            }
        }
    }

    public enum class Region private constructor(val code: Byte) {
        ALL(0xFF.toByte()),
        US_EAST(0),
        US_WEST(1),
        SOUTH_AMERICA(2),
        EUROPE(3),
        ASIA(4),
        AUSTRALIA(5),
        MIDDLE_EAST(6),
        AFRICA(7)
    }

    companion object {
        public val SOURCE: MasterServer = MasterServer("hl2master.steampowered.com", 27011)
        private val LOG = Logger()
    }
}
