package com.timepath.steam.io.bvdf

import com.timepath.DateUtils
import com.timepath.io.OrderedInputStream
import com.timepath.with
import java.awt.Color
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Tokenizes binary BDF files and notifies a listener
 * @see [https://github.com/harvimt/steam_launcher/blob/master/binvdf.py](null)
 * @see [https://github.com/barneygale/bvdf/blob/master/bvdf.py](null)
 * @see [https://github.com/DHager/hl2parse](null)
 * @see [http://cs.rin.ru/forum/viewtopic.php?f=20&t=61506&hilit=appinfo](null)
 * @see [http://cs.rin.ru/forum/viewtopic.php?f=20&t=62438&hilit=packageinfo](null)
 * @see [http://media.steampowered.com/steamcommunity/public/images/apps/[appID]/[sha].[ext]](null)
 * @see [http://cdr.xpaw.ru/app/5/.section_info](null)
 * @see [http://hlssmod.net/he_code/public/tier1/KeyValues.h](null)
 * @see [http://hpmod.googlecode.com/svn/trunk/tier1/KeyValues.cpp](null)
 */
public class BVDFStream @throws(IOException::class)
constructor(`in`: InputStream, private val listener: BVDFListener) {
    private val ois = OrderedInputStream(`in`) with { order(ByteOrder.LITTLE_ENDIAN) }

    throws(IOException::class)
    public fun read() {
        while (true) {
            val type = ValueType.read(ois)
            if (type == null) {
                // Parsing error
                LOG.log(Level.SEVERE, "Type: {0}, position: {1}", arrayOf(type, ois.position())) // TODO: pushback header
                return
            }
            when (type) {
                BVDFStream.ValueType.TYPE_APPINFO -> {
                    listener.emit("universe", BVDFConstants.Universe.getName(ois.readInt()))
                    while (true) {
                        val appID = ois.readInt()
                        if (appID == 0) {
                            break
                        }
                        listener.push(appID)
                        run {
                            // Improvement: lazy load entries
                            val blockSize = ois.readInt()
                            val appInfoState = ois.readInt()
                            listener.emit("state", BVDFConstants.AppInfoState.getName(appInfoState))
                            val lastUpdated = ois.readInt().toLong()
                            val formattedDate = DateUtils.parse(lastUpdated)
                            listener.emit("lastUpdated", formattedDate)
                            val token = ois.readLong()
                            listener.emit("token", token)
                            val sha = ByteArray(20)
                            ois.readFully(sha)
                            listener.emit("sha", sha)
                            val changeNumber = ois.readInt()
                            listener.emit("changeNumber", changeNumber)
                            listener.push("Sections")
                            run {
                                while (true) {
                                    val section = ois.read()
                                    if (section == 0) {
                                        break
                                    }
                                    listener.push(BVDFConstants.Section.get(section))
                                    run {
                                        // Read regular data recursively from here
                                        read()
                                    }
                                    listener.pop()
                                }
                            }
                            listener.pop()
                        }
                        listener.pop()
                    }
                    return
                }
                BVDFStream.ValueType.TYPE_PACKAGEINFO -> {
                    listener.emit("universe", BVDFConstants.Universe.getName(ois.readInt()))
                    while (true) {
                        val packageID = ois.readInt()
                        if (packageID == -1) {
                            break
                        }
                        listener.push(packageID)
                        val sha = ByteArray(20)
                        ois.readFully(sha)
                        listener.emit("sha", sha)
                        val changeNumber = ois.readInt()
                        listener.emit("changeNumber", changeNumber)
                        read()
                        listener.pop()
                    }
                    return
                }
                BVDFStream.ValueType.TYPE_NUMTYPES -> // Leave node
                    return
            }
            val key = ois.readString()
            when (type) {
                BVDFStream.ValueType.TYPE_NONE -> {
                    listener.push(key)
                    read()
                    listener.pop()
                }
                BVDFStream.ValueType.TYPE_STRING -> listener.emit(key, ois.readString())
                BVDFStream.ValueType.TYPE_WSTRING -> {
                    LOG.log(Level.WARNING, "Detected {0}, this should never happen", type)
                    listener.emit(key, ois.readString())
                }
                BVDFStream.ValueType.TYPE_INT, BVDFStream.ValueType.TYPE_PTR -> listener.emit(key, ois.readInt())
                BVDFStream.ValueType.TYPE_UINT64 -> listener.emit(key, ois.readLong())
                BVDFStream.ValueType.TYPE_FLOAT -> listener.emit(key, ois.readFloat())
                BVDFStream.ValueType.TYPE_COLOR -> {
                    ois.order(ByteOrder.BIG_ENDIAN)
                    val color = Color(ois.readInt(), true)
                    ois.order(ByteOrder.LITTLE_ENDIAN)
                    listener.emit(key, color)
                }
                else -> LOG.log(Level.SEVERE, "Unhandled data type {0}", type)
            }
        }
    }

    enum class ValueType(vararg sig: Int) {
        TYPE_NONE(0), TYPE_STRING(1), TYPE_INT(2), TYPE_FLOAT(3), TYPE_PTR(4), TYPE_WSTRING(5), TYPE_COLOR(6),
        TYPE_UINT64(7), TYPE_NUMTYPES(8),
        TYPE_APPINFO(0x26, 0x44, 0x56, 0x07),
        TYPE_PACKAGEINFO(0x27, 0x55, 0x56, 0x06);

        val sig: IntArray = sig

        companion object {

            throws(IOException::class)
            public fun read(ois: OrderedInputStream): ValueType? {
                val i = ois.read()
                for (type in ValueType.values()) {
                    if (type.sig[0] == i) {
                        if (type.sig.size() > 1) {
                            ois.readFully(ByteArray(type.sig.size() - 1))
                        }
                        return type
                    }
                }
                return null
            }
        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<BVDFStream>().getName())
    }
}
