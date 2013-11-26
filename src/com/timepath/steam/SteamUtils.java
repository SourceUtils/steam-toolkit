package com.timepath.steam;

import com.timepath.plaf.OS;
import com.timepath.plaf.win.WinRegistry;
import com.timepath.steam.io.VDF1;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class SteamUtils {

    private static final Logger LOG = Logger.getLogger(SteamUtils.class.getName());

    private SteamUtils() {
    }

    public static SteamID getUser() {
        File autoLogin = new File(SteamUtils.getSteam(), "config/SteamAppData.vdf");
        File config = new File(SteamUtils.getSteam(), "config/config.vdf");
        if(!autoLogin.exists() || !config.exists()) {
            return null;
        }
        try {
            VDF1 u = new VDF1();
            u.setLogLevel(Level.ALL);
            u.readExternal(new FileInputStream(autoLogin));
            String username = u.getRoot().get("SteamAppData").get("AutoLoginUser").getValue();
            VDF1 i = new VDF1();
            i.setLogLevel(Level.ALL);
            i.readExternal(new FileInputStream(config));
            String id64 = i.getRoot().get("InstallConfigStore").get("Software").get("Valve").get(
                "Steam").get("Accounts").get(username).get("SteamID").getValue();
            String uid = SteamID.ID64toUID(id64);
            String sid = SteamID.UIDtoID32(uid);
            return new SteamID(username, id64, uid, sid);
        } catch(FileNotFoundException ex) {
            Logger.getLogger(SteamUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static File getUserData(SteamID user) {
        if(user == null) {
            return null;
        }
        File steam = getSteam();
        if(steam == null) {
            return null;
        }
        return new File(steam, "userdata/" + user.getUID().split(":")[2]);
    }

    public static File getUserData() {
        return getUserData(getUser());
    }

    /**
     * Todo: libraries:
     * Steam/config/config.vdf:InstallConfigStore/Software/Valve/Steam/BaseInstallFolder_X
     * <p/>
     * @return
     */
    public static File getSteamApps() {
        File steam = getSteam();
        if(steam == null) {
            return null;
        }
        switch(OS.get()) {
            case Windows:
                return new File(steam, "steamapps");
            case OSX:
            case Linux:
                return new File(steam, "SteamApps");
            default:
                return null;
        }
    }

    public static File getSteam() {
        switch(OS.get()) {
            case Windows:
                String reg = null;
                try {
                    reg = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER,
                                                 "Software\\Valve\\Steam", "SteamPath");
                } catch(Exception ex) {
                    LOG.log(Level.WARNING, "Windows registry read failed", ex);
                }
                if(reg != null) {
                    LOG.log(Level.INFO, "Steam directory read from registry: {0}", reg);
                    return new File(reg);
                }

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
                File macReg = new File("~/Library/Application Support/Steam/registry.vdf");
                if(macReg.exists()) {
                    VDF1 v = new VDF1();
                    try {
                        v.readExternal(new FileInputStream(macReg));
                        String installPath = v.getRoot().get("Registry").get("HKLM").get("Software").get(
                            "Valve").get("Steam").get("InstallPath").getValue();
                        LOG.log(Level.INFO, "Steam directory read from registry file: {0}",
                                installPath);
                        return new File(installPath);
                    } catch(FileNotFoundException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
                return new File("~/Library/Application Support/Steam");
            case Linux:
                File linReg = new File(System.getenv("HOME") + "/.steam/registry.vdf");
                if(linReg.exists()) {
                    VDF1 v = new VDF1();
                    try {
                        v.readExternal(new FileInputStream(linReg));
                        String installPath = v.getRoot().get("Registry").get("HKLM").get("Software").get(
                            "Valve").get("Steam").get("InstallPath").getValue();
                        LOG.log(Level.INFO, "Steam directory read from registry file: {0}",
                                installPath);
                        return new File(installPath);
                    } catch(FileNotFoundException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
                return new File(System.getenv("HOME") + "/.steam/steam"); // shouldn't this be correct regardess?
            default:
                return null;
        }
    }

}
