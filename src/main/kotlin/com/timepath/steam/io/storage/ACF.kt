package com.timepath.steam.io.storage

import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDF
import com.timepath.vfs.provider.local.LocalFileProvider
import java.io.File
import java.io.IOException
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.HashMap

/**
 * @author TimePath
 */
public class ACF private(root: File) : LocalFileProvider(root) {
    companion object {
        private val REFERENCE_MAP: MutableMap<String, Reference<ACF>> = HashMap()

        throws(javaClass<IOException>())
        public fun fromManifest(appID: Int): ACF = fromManifest(File(SteamUtils.getSteamApps(), "appmanifest_$appID.acf"))

        throws(javaClass<IOException>())
        private fun fromManifest(mf: File): ACF {
            val vdf = VDF.load(mf)
            val dir: File
            try {
                dir = File(vdf["AppState"]!!.getValue("appinstalldir") as String)
            } catch (ignored: Exception) {
                dir = File(mf.getParentFile(), "common/${vdf["AppState"]!!.getValue("installdir")}")
            }

            // TODO: gameinfo.txt
            val key = mf.getName()
            return REFERENCE_MAP[key]?.get() ?: ACF(dir).let {
                REFERENCE_MAP[key] = SoftReference(it)
                it
            }
        }
    }
}
