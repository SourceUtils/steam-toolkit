package com.timepath.steam.io.storage;

import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.ProviderPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.MetaInfServices;

@MetaInfServices(ProviderPlugin.class)
public class VPKPlugin implements ProviderPlugin {

    @NotNull
    @Override
    public SimpleVFile.FileHandler register() {
        return new SimpleVFile.FileHandler() {
            @Nullable
            @Override
            public Collection<SimpleVFile> handle(@NotNull File file) throws IOException {
                if (!file.getName().endsWith("_dir.vpk")) return null;
                @Nullable VPK ar = VPK.loadArchive(file);
                if (ar == null) return null;
                return ar.list();
            }
        };
    }
}
