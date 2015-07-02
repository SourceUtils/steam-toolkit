package com.timepath.steam.io.storage;

import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.ProviderPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.MetaInfServices;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@MetaInfServices(ProviderPlugin.class)
public class VPKPlugin implements ProviderPlugin {

    @NotNull
    @Override
    public SimpleVFile.FileHandler register() {
        return new SimpleVFile.FileHandler() {
            @Override
            public Collection<SimpleVFile> handle(@NotNull File file) throws IOException {
                if (!file.getName().endsWith("_dir.vpk")) return null;
                VPK ar = VPK.Companion.loadArchive(file);
                if (ar == null) return null;
                return ar.list();
            }
        };
    }
}
