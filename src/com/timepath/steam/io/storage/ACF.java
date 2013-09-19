package com.timepath.steam.io.storage;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class ACF extends Files {  
    
    public ACF(File root) {
        super(root);
    }

    public static ACF fromManifest(File manifest) throws FileNotFoundException {
        VDF v = new VDF();
        v.readExternal(new FileInputStream(manifest));
        File appInstallDir = new File(v.getRoot().get("AppState").get("UserConfig").get("appinstalldir").getValue());
        // TODO: gameinfo.txt
        return new ACF(appInstallDir);
    }
    
    public static ACF fromManifest(int appID) throws FileNotFoundException {
        return fromManifest(new File(SteamUtils.getSteamApps(), "appmanifest_" + appID + ".acf"));
    }

    private static final Logger LOG = Logger.getLogger(ACF.class.getName());

}
