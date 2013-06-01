package com.timepath.steam;

import com.timepath.plaf.OS;
import com.timepath.steam.io.VDF;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class SteamUtils {

    private static final Logger LOG = Logger.getLogger(SteamUtils.class.getName());

    private SteamUtils() {
    }
    
    public static SteamID getUser() {
        try {
            VDF u = new VDF();
            u.setLogLevel(Level.ALL);
            u.readExternal(new FileInputStream(new File(SteamUtils.getSteam(), "config/SteamAppData.vdf")));
            String username = u.getRoot().get("SteamAppData").get("AutoLoginUser").getValue();
            VDF i = new VDF();
            i.setLogLevel(Level.ALL);
            i.readExternal(new FileInputStream(new File(SteamUtils.getSteam(), "config/config.vdf")));
            String id64 = i.getRoot().get("InstallConfigStore").get("Software").get("Valve").get("Steam").get("Accounts").get(username).get("SteamID").getValue();
            String uid = SteamID.ID64toUID(id64);
            String sid = SteamID.UIDtoID32(uid);
            return new SteamID(username, id64, uid, sid);
        } catch(FileNotFoundException ex) {
            Logger.getLogger(SteamUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static File getSteamApps() {
        switch(OS.get()) {
            case Windows:
                return new File(getSteam(), "steamapps");
            case OSX:
            case Linux:
                return new File(getSteam(), "SteamApps");
            default:
                return null;
        }
    }

    public static File getSteam() {
        switch(OS.get()) {
            case Windows:
                String str = System.getenv("PROGRAMFILES(x86)");
                if(str == null) {
                    str = System.getenv("PROGRAMFILES");
                }
                File f = new File(str, "Steam");
                if(f.exists()) {
                    return f;
                }
                String sourcesdk = System.getenv("sourcesdk");
                if(sourcesdk != null) {
                    sourcesdk = sourcesdk.toLowerCase();
                    String scan = "steam";
                    int idx = sourcesdk.indexOf(scan);
                    if(idx != -1) {
                        return new File(sourcesdk.substring(0, idx + scan.length()));
                    }
                }
                return null;
            case OSX:
                return new File("~/Library/Application Support/Steam");
            case Linux:
                return new File(System.getenv("HOME") + "/.steam/steam");
            default:
                return null;
        }
    }
}
