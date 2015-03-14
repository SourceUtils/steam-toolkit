package com.timepath.steam.io.storage;

import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.ProviderPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.MetaInfServices;

import java.io.File;
import java.util.Collection;

/**
* @author TimePath
*/
@MetaInfServices(ProviderPlugin.class)
public class VPKPlugin implements ProviderPlugin {
    @NotNull
    @Override
    public SimpleVFile.FileHandler register() {
        return new SimpleVFile.FileHandler() {
            @Nullable
            @Override
            public Collection<? extends SimpleVFile> handle(@NotNull File file) {
                if (!file.getName().endsWith("_dir.vpk")) return null;
                return VPK.loadArchive(file).list();
            }
        };
    }
}
