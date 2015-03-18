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

    public fun isBinary(f: File): Boolean = try {
        val rf = RandomAccessFile(f, "r")
        rf.seek(rf.length() - 1)
        val r = rf.read()
        (r == 0x00) || (r == 0x08) || (r == 0xFF)
    } catch (ex: IOException) {
        Logger.getLogger(javaClass<VDF>().getName()).log(Level.SEVERE, null, ex)
        false
    }

    throws(javaClass<IOException>())
    public fun load(file: File, charset: Charset = Charsets.UTF_8): VDFNode = load(FileInputStream(file), charset)

    throws(javaClass<IOException>())
    public fun load(input: InputStream, charset: Charset = Charsets.UTF_8): VDFNode = VDFNode(input, charset)

}
