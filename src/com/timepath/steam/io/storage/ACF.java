package com.timepath.steam.io.storage;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class ACF extends Files {

    private static final Logger                      LOG   = Logger.getLogger(ACF.class.getName());
    private static final Map<String, Reference<ACF>> cache = new HashMap<>();

    private ACF(File root) {
        super(root);
    }

    public static ACF fromManifest(int appID) throws FileNotFoundException {
        return fromManifest(new File(SteamUtils.getSteamApps(), "appmanifest_" + appID + ".acf"));
    }

    private static ACF fromManifest(File mf) throws FileNotFoundException {
        VDF1 v = new VDF1(); v.readExternal(new FileInputStream(mf)); File dir; try {
            dir = new File(v.getRoot().get("AppState").get("UserConfig").get("appinstalldir").getValue());
        } catch(Exception e) {
            dir = new File(mf.getParentFile(), "common/" + v.getRoot().get("AppState").get("installdir").getValue());
        }
        // TODO: gameinfo.txt
        String key = mf.getName(); Reference<ACF> ref = cache.get(key); if(ref != null) {
            ACF a = ref.get(); if(a != null) {
                return a;
            }
        } ACF a = new ACF(dir); cache.put(key, new SoftReference<>(a)); return a;
    }
}
