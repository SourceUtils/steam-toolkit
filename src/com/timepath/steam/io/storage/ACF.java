package com.timepath.steam.io.storage;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class ACF extends Files {

    private static final Logger LOG = Logger.getLogger(ACF.class.getName());

    private static final HashMap<String, SoftReference<ACF>> cache = new HashMap<String, SoftReference<ACF>>();

    public static ACF fromManifest(File manifest) throws FileNotFoundException {
        VDF v = new VDF();
        v.readExternal(new FileInputStream(manifest));
        File appInstallDir = new File(v.getRoot().get("AppState").get("UserConfig").get("appinstalldir")
            .getValue());
        // TODO: gameinfo.txt

        String key = manifest.getName();
        if(cache.containsKey(key)) {
            SoftReference<ACF> ref = cache.get(key);
            ACF a = ref.get();
            if(a != null) {
                return a;
            }
        }
        ACF a = new ACF(appInstallDir);
        cache.put(key, new SoftReference<ACF>(a));
        return a;
    }

    public static ACF fromManifest(int appID) throws FileNotFoundException {
        return fromManifest(new File(SteamUtils.getSteamApps(), "appmanifest_" + appID + ".acf"));
    }

    public ACF(File root) {
        super(root);
    }

}
