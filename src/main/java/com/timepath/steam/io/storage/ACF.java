package com.timepath.steam.io.storage;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.VDFNode;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class ACF extends Files {

    private static final Logger                      LOG           = Logger.getLogger(ACF.class.getName());
    private static final Map<String, Reference<ACF>> REFERENCE_MAP = new HashMap<>();

    private ACF(File root) {
        super(root);
    }

    public static ACF fromManifest(int appID) throws IOException {
        return fromManifest(new File(SteamUtils.getSteamApps(), "appmanifest_" + appID + ".acf"));
    }

    private static ACF fromManifest(File mf) throws IOException {
        VDFNode v = VDF.load(mf);
        File dir;
        try {
            dir = new File((String) v.get("AppState", "UserConfig").getValue("appinstalldir"));
        } catch(Exception ignored) {
            dir = new File(mf.getParentFile(), "common/" + v.get("AppState").getValue("installdir"));
        }
        // TODO: gameinfo.txt
        String key = mf.getName();
        Reference<ACF> ref = REFERENCE_MAP.get(key);
        if(ref != null) {
            ACF a = ref.get();
            if(a != null) {
                return a;
            }
        }
        ACF a = new ACF(dir);
        REFERENCE_MAP.put(key, new SoftReference<>(a));
        return a;
    }
}
