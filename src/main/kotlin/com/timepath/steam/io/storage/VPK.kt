package com.timepath.steam.io.storage

import com.timepath.DataUtils
import com.timepath.StringUtils
import com.timepath.io.ByteBufferInputStream
import com.timepath.io.struct.StructField
import com.timepath.toUnsigned
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ExtendedVFile
import com.timepath.with
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.nio.ByteBuffer
import java.util.HashMap
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.CRC32

/**
 * Loads _dir.vpk files
 * @see [https://developer.valvesoftware.com/wiki/VPK_File_Format](null)
 */
public class VPK @throws(IOException::class)
private constructor(file: File) : ExtendedVFile() {
    override val root = this

    private val globaldata: ByteBuffer
    private val mappings: Array<ByteBuffer?>
    private val store: Array<File?>
    override val name: String

    init {
        LOG.log(Level.INFO, "Loading {0}", file)
        name = file.getName()
                .let { it.substringBeforeLast(".vpk", it) }
                .let { it.substringBeforeLast("_dir", it) }
        val parts = file.getParentFile().listFiles(FileFilter {
            it != file
                    && it.getName().startsWith(name)
                    && it.getName().length() == name.length() + "_000.vpk".length()
        })
        store = arrayOfNulls<File>(parts.size())
        mappings = arrayOfNulls<ByteBuffer>(store.size())
        for (part in parts) {
            val split = part.getName().splitBy("_")
            val idx = Integer.parseInt(split[split.size() - 1].replace(".vpk", ""))
            store[idx] = part
        }
        val buffer = DataUtils.mapFile(file)
        val signature = buffer.getInt()
        if (signature != HEADER) {
            throw IOException("Not a VPK file")
        }
        val ver = buffer.getInt()
        val treeLength = buffer.getInt() // Unsigned length of directory slice
        val dataLength: Int
        val v2: Int
        val v3: Int // 48 in most
        val v4: Int
        if (ver >= 2) {
            dataLength = buffer.getInt()
            v2 = buffer.getInt()
            v3 = buffer.getInt()
            v4 = buffer.getInt()
        } else {
            dataLength = 0
            v2 = 0
            v3 = 0
            v4 = 0
        }
        val directoryInfo = DataUtils.getSlice(buffer, treeLength)
        globaldata = DataUtils.getSlice(buffer, dataLength)
        buffer.get(ByteArray(v2)) // Directory
        buffer.get(ByteArray(v3)) // Single + Directory
        buffer.get(ByteArray(v4)) // Directory
        val debug = arrayOf(
                arrayOf<Any>("dataLength = ", dataLength),
                arrayOf<Any>("v2 = ", v2),
                arrayOf<Any>("v3 = ", v3),
                arrayOf<Any>("v4 = ", v4),
                arrayOf<Any>("Underflow = ", buffer.remaining())
        )
        LOG.info(StringUtils.fromDoubleArray(debug, "Debug:"))
        parseTree(directoryInfo)
    }

    private inline fun parse(buffer: ByteBuffer, body: (String) -> Unit) {
        while (true) {
            val it = DataUtils.readZeroString(buffer)
            if (it.isEmpty()) break
            body(it)
        }
    }

    private fun parseTree(buffer: ByteBuffer) {
        parse(buffer) {
            val ext = when (it) {
                " " -> ""
                else -> '.' + it
            }
            parse(buffer) { dir ->
                val p = nodeForPath(dir)
                parse(buffer) { basename ->
                    val e = readFileInfo(buffer, basename + ext)
                    p.add(e)
                }
            }
        }
    }

    private fun nodeForPath(path: String): SimpleVFile {
        if (path == " ") return root
        return path.replace('\\', '/').splitBy("/").fold(root as SimpleVFile) { parent, child ->
            parent.list().firstOrNull {
                it.isDirectory && it.name.equals(child, ignoreCase = true)
            } ?: run {
                VPKDirectoryEntry(child) with {
                    isDirectory = true
                    parent.add(this)
                }
            }
        }
    }

    private fun readFileInfo(buffer: ByteBuffer, name: String): VPKDirectoryEntry {
        val e = VPKDirectoryEntry(name) with {
            crc = buffer.getInt()
            preloadBytes = buffer.getShort()
            archiveIndex = buffer.getShort()
            entryOffset = buffer.getInt()
            entryLength = buffer.getInt()
        }
        buffer.position(buffer.position() + e.preloadBytes) // TODO: load preload bytes
        val term = buffer.getShort().toUnsigned()
        assert(term == 0xFFFF, "VPK directory reading failed")
        return e
    }

    override val isDirectory = true

    private fun getData(i: Int): ByteBuffer? {
        try {
            return mappings[i] ?: DataUtils.mapFile(store[i]!!) with { mappings[i] = this }
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        return null
    }

    override val attributes = null

    override val isComplete = true

    override fun openStream() = null

    /**
     * If a file contains preload data, the preload data immediately follows the
     * above structure. The entire size of a file is PreloadBytes + EntryLength.
     */
    private inner class VPKDirectoryEntry(override var name: String) : ExtendedVFile() {

        /**
         * A 32bit CRC of the file's data.
         */
        StructField(0) var crc: Int = 0
        StructField(1) var preloadBytes: Short = 0
        StructField(2) var archiveIndex: Short = 0
        StructField(3) var entryOffset: Int = 0
        StructField(4) var entryLength: Int = 0

        override var isDirectory: Boolean = false
        private var localdata: Reference<ByteBuffer>? = null

        override fun calculateChecksum(): Long {
            if (localData() == null) {
                return 0
            }
            val checksum = CRC32()
            localData()!!.position(0)
            val buf = ByteArray(4096)
            while (localData()!!.hasRemaining()) {
                val bsize = Math.min(buf.size(), localData()!!.remaining())
                localData()!!.get(buf, 0, bsize)
                checksum.update(buf, 0, bsize)
            }
            return checksum.getValue()
        }

        override val attributes = null

        override val checksum: Long get() = crc.toLong()

        override val root = this@VPK

        public fun getSource(): ByteBuffer? {
            if (archiveIndex.toInt() == 0x7FFF) {
                // This archive
                return globaldata
            }
            return getData(archiveIndex.toInt())
        }

        override val isComplete: Boolean get() {
            val theoretical = crc.toLong()
            val real = calculateChecksum()
            return theoretical == real
        }

        override val length: Long get() = entryLength.toLong()

        public fun localData(): ByteBuffer? {
            localdata?.get()?.let { return it }
            return DataUtils.getSlice(getSource()!!.with { position(entryOffset) }, entryLength) with {
                localdata = SoftReference(this)
            }
        }

        override fun openStream() = ByteBufferInputStream(localData()!!)
    }

    companion object {

        private val HEADER = 0x55AA1234
        private val LOG = Logger.getLogger(javaClass<VPK>().getName())
        /**
         * Previously loaded VPKs stored as references.
         */
        private val REFERENCE_MAP = HashMap<File, Reference<VPK>>(0)

        public fun loadArchive(file: File): VPK? {
            REFERENCE_MAP[file]?.get()?.let {
                LOG.log(Level.INFO, "Loaded {0} from cache", file)
                return it
            }
            try {
                return VPK(file) with { REFERENCE_MAP[file] = SoftReference(this) }
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
                return null
            }
        }
    }
}
