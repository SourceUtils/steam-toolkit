package com.timepath.steam.io.storage;

import com.timepath.vfs.provider.Files;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author TimePath
 * @see <a>http://wiki.singul4rity.com/steam:filestructures:ncf</a>
 */
public class NCF extends Files {

    public NCF(@NotNull File f) {
        super(f);
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
