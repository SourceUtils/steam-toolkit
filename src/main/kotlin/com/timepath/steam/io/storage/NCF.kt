package com.timepath.steam.io.storage

import com.timepath.vfs.provider.local.LocalFileProvider

import java.io.File

/**
 * @see <a>http://wiki.singul4rity.com/steam:filestructures:ncf</a>
 */
public class NCF(f: File) : LocalFileProvider(f) {

    init {
        throw UnsupportedOperationException("Not supported yet.")
    }
}
