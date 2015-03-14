package com.timepath.steam.io


import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
object VDF {

    public fun isBinary(f: File): Boolean {
        try {
            val rf = RandomAccessFile(f, "r")
            rf.seek(rf.length() - 1)
            val r = rf.read()
            return (r == 0x00) || (r == 0x08) || (r == 0xFF)
        } catch (ex: IOException) {
            Logger.getLogger(javaClass<VDF>().getName()).log(Level.SEVERE, null, ex)
        }
        return false
    }

    throws(javaClass<IOException>())
    public fun load(f: File, c: Charset = Charsets.UTF_8): VDFNode {
        return load(FileInputStream(f), c)
    }

    throws(javaClass<IOException>())
    public fun load(`is`: InputStream, c: Charset = Charsets.UTF_8): VDFNode {
        return VDFNode(`is`, c)
    }

}
