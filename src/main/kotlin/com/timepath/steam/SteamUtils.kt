package com.timepath.steam

import com.timepath.plaf.OS
import com.timepath.plaf.win.WinRegistry
import com.timepath.steam.io.VDF

import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.platform.platformStatic

/**
 * Utility class for accessing local steam file locations
 *
 * @author TimePath
 */
public object SteamUtils {

    private val LOG = Logger.getLogger(javaClass<SteamUtils>().getName())

    /**
     * @return The most recently logged in steam user, or null
     */
    public fun getUser(): SteamID? {
        val autoLogin = File(getSteam(), "config/SteamAppData.vdf")
        val config = File(getSteam(), "config/config.vdf")
        if (!autoLogin.exists() || !config.exists()) {
            return null
        }
        try {
            val username = VDF.load(autoLogin).get("SteamAppData")!!.getValue("AutoLoginUser") as String
            val id64 = VDF.load(config).get("InstallConfigStore", "Software", "Valve", "Steam", "Accounts", username)!!.getValue("SteamID") as String
            val uid = SteamID.ID64toUID(id64)
            val sid = SteamID.UIDtoID32(uid!!)
            return SteamID(username, id64, uid, sid!!)
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        return null
    }

    /**
     * @return Path to /Steam/ installation
     */
    public fun getSteam(): File? = when (OS.get()) {
        OS.Linux -> getSteamLinux()
        OS.OSX -> getSteamOSX()
        OS.Windows -> getSteamWindows()
        else -> null
    }

    private fun getSteamLinux(): File {
        val linReg = File(System.getenv("HOME") + "/.steam/registry.vdf")
        try {
            val VDFNode = VDF.load(linReg)
            val installPath = VDFNode
                    .get("Registry", "HKLM", "Software", "Valve", "Steam")!!
                    .getValue("InstallPath") as String
            LOG.log(Level.INFO, "Steam directory read from registry file: {0}", installPath)
            return File(installPath)
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        return File(System.getenv("HOME") + "/.steam/steam") // TODO: Shouldn't this be correct regardess?
    }

    private fun getSteamOSX(): File {
        val macReg = File("~/Library/Application Support/Steam/registry.vdf")
        try {
            val installPath = VDF.load(macReg).get("Registry", "HKLM", "Software", "Valve", "Steam")!!.getValue("InstallPath") as String
            LOG.log(Level.INFO, "Steam directory read from registry file: {0}", installPath)
            return File(installPath)
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        return File("~/Library/Application Support/Steam")
    }

    private fun getSteamWindows(): File? {
        var reg: String? = null
        try {
            reg = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER, "Software\\Valve\\Steam", "SteamPath")
        } catch (ex: Exception) {
            LOG.log(Level.WARNING, "Windows registry read failed", ex)
        }

        if (reg != null) {
            LOG.log(Level.FINE, "Steam directory read from registry: {0}", reg)
            return File(reg)
        }
        var programFiles: String? = System.getenv("PROGRAMFILES(x86)")
        if (programFiles == null) {
            // Not a 64 bit machine
            programFiles = System.getenv("PROGRAMFILES")
        }
        val f = File(programFiles, "Steam")
        if (f.exists()) {
            return f
        }
        // Still haven't found steam
        LOG.log(Level.WARNING, "Steam directory not found, trying alternate detection methods")
        var sourcesdk: String? = System.getenv("sourcesdk")
        if (sourcesdk != null) {
            sourcesdk = sourcesdk!!.toLowerCase()
            val scan = "steam"
            val idx = sourcesdk!!.indexOf(scan)
            if (idx != -1) {
                val ret = File(sourcesdk!!.substring(0, idx + scan.length()))
                LOG.log(Level.INFO, "Found steam via sourcesdk env var: {0}", ret)
                return ret
            }
        }
        return null
    }

    /**
     * @return Path to /Steam/SteamApps/
     */
    public platformStatic fun getSteamApps(): File? {
        val steam = getSteam()
        if (steam == null) {
            return null
        }
        // TODO: libraries: Steam/config/config.vdf:InstallConfigStore/Software/Valve/Steam/BaseInstallFolder_X
        return when (OS.get()) {
            OS.Linux, OS.OSX -> File(steam, "SteamApps")
            OS.Windows -> File(steam, "steamapps")
            else -> null
        }
    }

    /**
     * @param user The user
     * @return Path to {@code user}'s {@code userdata}, or null
     * @throws IllegalArgumentException If {@code user} is null
     */
    public fun getUserData(user: SteamID?): File? {
        if (user == null) {
            throw IllegalArgumentException("User cannot be null")
        }
        val steam = getSteam()
        if (steam == null) {
            return null
        }
        return File(steam, "userdata/" + user.UID.split(":")[2])
    }

    /**
     * @return Path to {@link #getUser()}'s {@code userdata}
     */
    public fun getUserData(): File? {
        return getUserData(getUser())
    }
}
