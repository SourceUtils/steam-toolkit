package com.timepath.steam.io.storage

import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ProviderPlugin
import org.kohsuke.MetaInfServices

import java.io.File

/**
 * @author TimePath
 */
MetaInfServices(javaClass<ProviderPlugin>())
public class VPKPlugin : ProviderPlugin {
    override fun register() = object : SimpleVFile.FileHandler {
        override fun handle(file: File): Collection<SimpleVFile>? {
            if (!file.getName().endsWith("_dir.vpk")) return null
            return VPK.loadArchive(file)!!.list()
        }
    }
}
