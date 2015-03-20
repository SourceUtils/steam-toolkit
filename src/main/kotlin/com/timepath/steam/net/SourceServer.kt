package com.timepath.steam.net

import com.timepath.DataUtils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 * @see <a>https://developer.valvesoftware.com/wiki/Server_Queries</a>
 */
public class SourceServer(hostname: String, port: Int) : Server(hostname, port) {

    throws(javaClass<IOException>())
    public fun getInfo(listener: ServerListener = ServerListener.NULL) {
        val baos = ByteArrayOutputStream()
        baos.write(HEADER)
        // A2S_INFO
        baos.write(84)
        baos.write(("Source Engine Query${0.toChar()}").toByteArray())
        send(ByteBuffer.wrap(baos.toByteArray()))
        val buf = get()
        buf.order(ByteOrder.LITTLE_ENDIAN)
        val packHeader = buf.getInt()
        if (packHeader != -1) {
            LOG.log(Level.SEVERE, "Invalid packet header {0}", packHeader)
        }
        val header = buf.get().toInt()
        if (header != 0x49) {
            LOG.log(Level.SEVERE, "Invalid header {0}", header)
        }
        val protocol = buf.get()
        listener.inform("Protocol: $protocol")
        val name = DataUtils.getString(buf)
        listener.inform("Name: '$name'")
        val map = DataUtils.getString(buf)
        listener.inform("Map: '$map'")
        val gamedir = DataUtils.getString(buf)
        listener.inform("Gamedir: '$gamedir'")
        val game = DataUtils.getString(buf)
        listener.inform("Game: '$game'")
        val appID = buf.getShort()
        listener.inform("AppID: '$appID'")
        val playerCount = buf.get()
        listener.inform("Players: '$playerCount'")
        val playerCountMax = buf.get()
        listener.inform("Capacity: '$playerCountMax'")
        val botCount = buf.get()
        listener.inform("Bots: '$botCount'")
        val type = ServerType[buf.get()]
        listener.inform("Type: '$type'")
        val env = Environment[buf.get()]
        listener.inform("Environment: '$env'")
        val visibility = buf.get() == 0.toByte()
        listener.inform("Visible: '$visibility'")
        val secure = buf.get() == 1.toByte()
        listener.inform("VAC: '$secure'")
        val version = DataUtils.getString(buf)
        listener.inform("Version: '$version'")
        val edf = buf.get().toInt()
        val edfPort = (edf and 0b10000000) != 0
        val edfSTV = (edf and 0b1000000) != 0
        val edfTags = (edf and 0b100000) != 0
        val edfSteamID = (edf and 0b10000) != 0
        val edfGameID = (edf and 0b1) != 0
        if (edfPort) {
            val portLocal = buf.getShort()
            listener.inform("Port: '$portLocal'")
        }
        if (edfSteamID) {
            // TODO: check
            //            ByteBuffer d = buf.duplicate();
            //            d.limit(buf.position() + 8);
            //            LOG.info(DataUtils.hexDump(d.slice()));
            var sid = BigInteger.valueOf(buf.getLong())
            if (sid.compareTo(BigInteger.ZERO) < 0) {
                sid = sid.add(BigInteger.ONE.shiftLeft(64))
            }
            listener.inform("SteamID: '$sid'")
        }
        if (edfSTV) {
            val stvPort = buf.getShort()
            listener.inform("STV Port: '$stvPort'")
            val stvName = DataUtils.getString(buf)
            listener.inform("STV Name: '$stvName'")
        }
        if (edfTags) {
            val tags = DataUtils.getString(buf)
            listener.inform("Tags: '$tags'")
        }
        if (edfGameID) {
            var gid = BigInteger.valueOf(buf.getLong())
            if (gid.compareTo(BigInteger.ZERO) < 0) {
                gid = gid.add(BigInteger.ONE.shiftLeft(64))
            }
            listener.inform("GameID: '$gid'")
        }
    }

    throws(javaClass<IOException>())
    public fun getRules(l: com.timepath.steam.net.ServerListener = ServerListener.NULL) {
        // Get a challenge key
        val challengeOut = ByteArrayOutputStream()
        challengeOut.write(HEADER)
        challengeOut.write(86)
        challengeOut.write(HEADER)
        val challengeSend = ByteBuffer.wrap(challengeOut.toByteArray())
        send(challengeSend)
        val challengeGet = get()
        challengeGet.order(ByteOrder.LITTLE_ENDIAN)
        val challengepackHeader = challengeGet.getInt()
        if (challengepackHeader != -1) {
            LOG.log(Level.SEVERE, "Invalid packet header {0}", challengepackHeader)
        }
        val challengeheader = challengeGet.get().toInt()
        if (challengeheader != 65) {
            LOG.log(Level.SEVERE, "Invalid header {0}", challengeheader)
        }
        val challengeKey = ByteArray(4)
        challengeGet[challengeKey]
        val baos = ByteArrayOutputStream()
        baos.write(HEADER)
        baos.write(86)
        baos.write(challengeKey)
        val send = ByteBuffer.wrap(baos.toByteArray())
        val ruleBuf = ByteBuffer.allocate(4000)
        ruleBuf.order(ByteOrder.LITTLE_ENDIAN)
        var ruleCount = 0
        while (true) {
            send.rewind()
            send(send)
            val buf = get()
            buf.order(ByteOrder.LITTLE_ENDIAN)
            val packHeader = buf.getInt()
            if (packHeader != -2) {
                LOG.log(Level.SEVERE, "Invalid packet header {0}", packHeader)
            }
            val reqID = buf.getInt()
            val fragments = buf.get().toInt()
            val id = buf.get().toInt() + 1 // zero-indexed
            val payloadLength = buf.getShort().toInt()
            if (id == 1) {
                val pack2Header = buf.getInt()
                if (pack2Header != -1) {
                    LOG.log(Level.SEVERE, "Invalid packHeader {0}", pack2Header)
                }
                val header = buf.get().toInt()
                if (header != 0x41) {
                    LOG.log(Level.SEVERE, "Invalid header {0}", header)
                }
                ruleCount = buf.getShort().toInt()
            }
            LOG.log(Level.FINE, "{0} / {1}", array<Any>(id, fragments))
            val data = ByteArray(buf.remaining())
            buf[data]
            ruleBuf.put(data)
            if (id == fragments) {
                break
            }
        }
        ruleBuf.flip()
        LOG.log(Level.FINE, "Rules: {0}", ruleCount)
        LOG.log(Level.FINE, "Remaining: {0}", ruleBuf.remaining())
        for (ruleIndex in 1..(ruleCount + 1) - 1) {
            if (ruleBuf.remaining() == 0) {
                break
            }
            val key = DataUtils.getString(ruleBuf)
            val value = DataUtils.getString(ruleBuf)
            l.inform("[$ruleIndex/$ruleCount] '$key' = '$value'")
        }
        LOG.log(Level.FINE, "Underflow: {0}", ruleBuf.remaining())
    }

    enum class ServerType(code: Char) {
        DEDICATED : ServerType('d')
        LISTEN : ServerType('l')
        SOURCE_TV : ServerType('p')

        val code: Byte = code.toByte()

        companion object {
            private val vals = values()
            fun get(b: Byte) = vals.firstOrNull { it.code == b }
        }
    }

    enum class Environment(code: Char) {
        WINDOWS : Environment('w')
        LINUX : Environment('l')

        val code: Byte = code.toByte()

        companion object {
            private val vals = values()
            fun get(b: Byte) = vals.firstOrNull { it.code == b }
        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<SourceServer>().getName())
        private val HEADER = byteArray(
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte()
        )
    }
}
