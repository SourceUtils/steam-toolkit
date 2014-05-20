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
 * Utility class for accessing local steam file locations
 *
 * @author TimePath
 */
public class SteamUtils {

    private static final Logger LOG = Logger.getLogger(SteamUtils.class.getName());

    private SteamUtils() {
    }

    /**
     * @return Path to /Steam/SteamApps/
     */
    public static File getSteamApps() {
        File steam = getSteam();
        if(steam == null) {
            return null;
        }
        // TODO: libraries: Steam/config/config.vdf:InstallConfigStore/Software/Valve/Steam/BaseInstallFolder_X
        switch(OS.get()) {
            case Linux:
            case OSX:
                return new File(steam, "SteamApps");
            case Windows:
                return new File(steam, "steamapps");
            default:
                return null;
        }
    }

    /**
     * @return Path to /Steam/ installation
     */
    public static File getSteam() {
        switch(OS.get()) {
            case Linux:
                return getSteamLinux();
            case OSX:
                return getSteamOSX();
            case Windows:
                return getSteamWindows();
            default:
                return null;
        }
    }

    private static File getSteamLinux() {
        File linReg = new File(System.getenv("HOME") + "/.steam/registry.vdf");
        if(linReg.exists()) {
            VDF1 v = new VDF1();
            try {
                v.readExternal(new FileInputStream(linReg));
                String installPath = v.getRoot()
                                      .get("Registry")
                                      .get("HKLM")
                                      .get("Software")
                                      .get("Valve")
                                      .get("Steam")
                                      .get("InstallPath")
                                      .getValue();
                LOG.log(Level.INFO, "Steam directory read from registry file: {0}", installPath);
                return new File(installPath);
            } catch(FileNotFoundException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        return new File(System.getenv("HOME") + "/.steam/steam"); // TODO: Shouldn't this be correct regardess?
    }

    private static File getSteamOSX() {
        File macReg = new File("~/Library/Application Support/Steam/registry.vdf");
        if(macReg.exists()) {
            VDF1 v = new VDF1();
            try {
                v.readExternal(new FileInputStream(macReg));
                String installPath = v.getRoot()
                                      .get("Registry")
                                      .get("HKLM")
                                      .get("Software")
                                      .get("Valve")
                                      .get("Steam")
                                      .get("InstallPath")
                                      .getValue();
                LOG.log(Level.INFO, "Steam directory read from registry file: {0}", installPath);
                return new File(installPath);
            } catch(FileNotFoundException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        return new File("~/Library/Application Support/Steam");
    }

    private static File getSteamWindows() {
        String reg = null;
        try {
            reg = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER, "Software\\Valve\\Steam", "SteamPath");
        } catch(Exception ex) {
            LOG.log(Level.WARNING, "Windows registry read failed", ex);
        }
        if(reg != null) {
            LOG.log(Level.FINE, "Steam directory read from registry: {0}", reg);
            return new File(reg);
        }
        String programFiles = System.getenv("PROGRAMFILES(x86)");
        if(programFiles == null) { // Not a 64 bit machine
            programFiles = System.getenv("PROGRAMFILES");
        }
        File f = new File(programFiles, "Steam");
        if(f.exists()) {
            return f;
        }
        // Still haven't found steam
        LOG.log(Level.WARNING, "Steam directory not found, trying alternate detection methods");
        String sourcesdk = System.getenv("sourcesdk");
        if(sourcesdk != null) {
            sourcesdk = sourcesdk.toLowerCase();
            String scan = "steam";
            int idx = sourcesdk.indexOf(scan);
            if(idx != -1) {
                File ret = new File(sourcesdk.substring(0, idx + scan.length()));
                LOG.log(Level.INFO, "Found steam via sourcesdk env var: {0}", ret);
                return ret;
            }
        }
        return null;
    }

    /**
     * @return The most recently logged in steam user, or null
     */
    public static com.timepath.steam.SteamID getUser() {
        File autoLogin = new File(getSteam(), "config/SteamAppData.vdf");
        File config = new File(getSteam(), "config/config.vdf");
        if(!autoLogin.exists() || !config.exists()) {
            return null;
        }
        try {
            VDF1 vdf1 = new VDF1();
            vdf1.setLogLevel(Level.ALL);
            vdf1.readExternal(new FileInputStream(autoLogin));
            String username = vdf1.getRoot().get("SteamAppData").get("AutoLoginUser").getValue();
            VDF1 vdf2 = new VDF1();
            vdf2.setLogLevel(Level.ALL);
            vdf2.readExternal(new FileInputStream(config));
            String id64 = vdf2.getRoot()
                              .get("InstallConfigStore")
                              .get("Software")
                              .get("Valve")
                              .get("Steam")
                              .get("Accounts")
                              .get(username)
                              .get("SteamID")
                              .getValue();
            String uid = com.timepath.steam.SteamID.ID64toUID(id64);
            String sid = com.timepath.steam.SteamID.UIDtoID32(uid);
            return new com.timepath.steam.SteamID(username, id64, uid, sid);
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * @param user
     *         The user
     *
     * @return Path to {@code user}'s {@code userdata}, or null
     *
     * @throws IllegalArgumentException
     *         If {@code user} is null
     */
    public static File getUserData(com.timepath.steam.SteamID user) {
        if(user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        File steam = getSteam();
        if(steam == null) {
            return null;
        }
        return new File(steam, "userdata/" + user.getUID().split(":")[2]);
    }

    /**
     * @return Path to {@link #getUser()}'s {@code userdata}
     */
    public static File getUserData() {
        return getUserData(getUser());
    }
}
