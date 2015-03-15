package com.timepath.steam.io.storage

import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDF
import com.timepath.steam.io.VDFNode
import com.timepath.vfs.provider.local.LocalFileProvider
import java.nio.charset.Charset

import java.io.File
import java.io.IOException
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.HashMap
import java.util.logging.Logger

/**
 * @author TimePath
 */
public class ACF private(root: File) : LocalFileProvider(root) {
    class object {

        private val LOG = Logger.getLogger(javaClass<ACF>().getName())
        private val REFERENCE_MAP = HashMap<String, Reference<ACF>>()

        throws(javaClass<IOException>())
        public fun fromManifest(appID: Int): ACF? {
            return fromManifest(File(SteamUtils.getSteamApps(), "appmanifest_" + appID + ".acf"))
        }

        throws(javaClass<IOException>())
        private fun fromManifest(mf: File): ACF? {
            val v = VDF.load(mf, Charset.defaultCharset())
            val dir: File
            try {
                dir = File(v["AppState"]!!.getValue("appinstalldir") as String)
            } catch (ignored: Exception) {
                dir = File(mf.getParentFile(), "common/" + v["AppState"]!!.getValue("installdir"))
            }

            // TODO: gameinfo.txt
            val key = mf.getName()
            val ref = REFERENCE_MAP[key]
            if (ref != null) {
                val a = ref.get()
                if (a != null) {
                    return a
                }
            }
            val a = ACF(dir)
            REFERENCE_MAP.put(key, SoftReference(a))
            return a
        }
    }
}
