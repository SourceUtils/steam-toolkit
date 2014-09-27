package com.timepath.steam;

import com.timepath.plaf.OS;
import com.timepath.plaf.win.WinRegistry;
import com.timepath.steam.io.VDF;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
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
     * @return The most recently logged in steam user, or null
     */
    @Nullable
    public static SteamID getUser() {
        @NotNull File autoLogin = new File(getSteam(), "config/SteamAppData.vdf");
        @NotNull File config = new File(getSteam(), "config/config.vdf");
        if (!autoLogin.exists() || !config.exists()) {
            return null;
        }
        try {
            @NotNull String username = (String) VDF.load(autoLogin).get("SteamAppData").getValue("AutoLoginUser");
            @NotNull String id64 = (String) VDF.load(config)
                    .get("InstallConfigStore", "Software", "Valve", "Steam", "Accounts", username)
                    .getValue("SteamID");
            @Nullable String uid = SteamID.ID64toUID(id64);
            @Nullable String sid = SteamID.UIDtoID32(uid);
            return new SteamID(username, id64, uid, sid);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * @return Path to /Steam/ installation
     */
    @Nullable
    public static File getSteam() {
        switch (OS.get()) {
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

    @NotNull
    private static File getSteamLinux() {
        @NotNull File linReg = new File(System.getenv("HOME") + "/.steam/registry.vdf");
        try {
            @NotNull String installPath = (String) VDF.load(linReg)
                    .get("Registry", "HKLM", "Software", "Valve", "Steam")
                    .getValue("InstallPath");
            LOG.log(Level.INFO, "Steam directory read from registry file: {0}", installPath);
            return new File(installPath);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return new File(System.getenv("HOME") + "/.steam/steam"); // TODO: Shouldn't this be correct regardess?
    }

    @NotNull
    private static File getSteamOSX() {
        @NotNull File macReg = new File("~/Library/Application Support/Steam/registry.vdf");
        try {
            @NotNull String installPath = (String) VDF.load(macReg)
                    .get("Registry", "HKLM", "Software", "Valve", "Steam")
                    .getValue("InstallPath");
            LOG.log(Level.INFO, "Steam directory read from registry file: {0}", installPath);
            return new File(installPath);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return new File("~/Library/Application Support/Steam");
    }

    @Nullable
    private static File getSteamWindows() {
        @Nullable String reg = null;
        try {
            reg = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER, "Software\\Valve\\Steam", "SteamPath");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Windows registry read failed", ex);
        }
        if (reg != null) {
            LOG.log(Level.FINE, "Steam directory read from registry: {0}", reg);
            return new File(reg);
        }
        String programFiles = System.getenv("PROGRAMFILES(x86)");
        if (programFiles == null) { // Not a 64 bit machine
            programFiles = System.getenv("PROGRAMFILES");
        }
        @NotNull File f = new File(programFiles, "Steam");
        if (f.exists()) {
            return f;
        }
        // Still haven't found steam
        LOG.log(Level.WARNING, "Steam directory not found, trying alternate detection methods");
        String sourcesdk = System.getenv("sourcesdk");
        if (sourcesdk != null) {
            sourcesdk = sourcesdk.toLowerCase();
            @NotNull String scan = "steam";
            int idx = sourcesdk.indexOf(scan);
            if (idx != -1) {
                @NotNull File ret = new File(sourcesdk.substring(0, idx + scan.length()));
                LOG.log(Level.INFO, "Found steam via sourcesdk env var: {0}", ret);
                return ret;
            }
        }
        return null;
    }

    /**
     * @return Path to /Steam/SteamApps/
     */
    @Nullable
    public static File getSteamApps() {
        @Nullable File steam = getSteam();
        if (steam == null) {
            return null;
        }
        // TODO: libraries: Steam/config/config.vdf:InstallConfigStore/Software/Valve/Steam/BaseInstallFolder_X
        switch (OS.get()) {
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
     * @param user The user
     * @return Path to {@code user}'s {@code userdata}, or null
     * @throws IllegalArgumentException If {@code user} is null
     */
    @Nullable
    public static File getUserData(@Nullable SteamID user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        @Nullable File steam = getSteam();
        if (steam == null) {
            return null;
        }
        return new File(steam, "userdata/" + user.getUID().split(":")[2]);
    }

    /**
     * @return Path to {@link #getUser()}'s {@code userdata}
     */
    @Nullable
    public static File getUserData() {
        return getUserData(getUser());
    }
}
