package com.timepath.steam.net

import com.timepath.DataUtils
import com.timepath.Utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 * @see <a>https://developer.valvesoftware.com/wiki/Master_Server_Query_Protocol</a>
 */
public class MasterServer(hostname: String, port: Int) : Server(hostname, port) {

    throws(javaClass<IOException>())
    public fun query(r: Region, filter: String = "", l: com.timepath.steam.net.ServerListener = ServerListener.NULL) {
        val initialAddress = "0.0.0.0:0"
        var lastAddress = initialAddress
        var looping = true
        while (looping) {
            LOG.log(Level.FINE, "Last address: {0}", lastAddress)
            val baos = ByteArrayOutputStream()
            baos.write(0x31)
            baos.write(r.code.toInt())
            baos.write((lastAddress + 0.toChar()).toByteArray())
            baos.write((filter + 0.toChar()).toByteArray())
            val send = ByteBuffer.wrap(baos.toByteArray())
            send(send)
            val buf = get()
            val header = buf.getInt()
            if (header != -1) {
                LOG.log(Level.WARNING, "Invalid header {0}", header)
                break
            }
            val head = buf.get()
            if (head.toInt() != 0x66) {
                LOG.log(Level.WARNING, "Unknown header {0}", head)
                val rec = DataUtils.hexDump(buf)
                LOG.log(Level.WARNING, "Received {0}", rec)
                l.inform(rec)
                break
            }
            val newline = buf.get()
            if (newline.toInt() != 0x0A) {
                LOG.log(Level.WARNING, "Malformed byte {0}", newline)
                break
            }
            val octet = IntArray(4)
            do {
                octet[0] = buf.get().toInt() and 0xFF
                octet[1] = buf.get().toInt() and 0xFF
                octet[2] = buf.get().toInt() and 0xFF
                octet[3] = buf.get().toInt() and 0xFF
                val serverPort = buf.getShort().toInt() and 0xFFFF
                lastAddress = "${octet[0]}.${octet[1]}.${octet[2]}.${octet[3]}:${serverPort}"
                (initialAddress != lastAddress).let {
                    looping = it
                    if (it) {
                        l.inform(lastAddress)
                    }
                }
            } while (buf.remaining() >= 6)
            if (buf.remaining() > 0) {
                val under = ByteArray(buf.remaining())
                if (under.size() > 0) {
                    LOG.log(Level.WARNING, "{0} byte underflow: {0}", array<Any>(buf.remaining(), Utils.hex(*under)))
                }
            }
        }
    }

    public enum class Region private(val code: Byte) {
        ALL : Region(255.toByte())
        US_EAST : Region(0)
        US_WEST : Region(1)
        SOUTH_AMERICA : Region(2)
        EUROPE : Region(3)
        ASIA : Region(4)
        AUSTRALIA : Region(5)
        MIDDLE_EAST : Region(6)
        AFRICA : Region(7)
    }

    class object {
        public val SOURCE: MasterServer = MasterServer("hl2master.steampowered.com", 27011)
        private val LOG = Logger.getLogger(javaClass<MasterServer>().getName())
    }
}
