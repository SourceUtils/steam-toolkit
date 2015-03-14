package com.timepath.steam


import java.math.BigInteger
import java.text.MessageFormat
import java.util.logging.Logger
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utility class for converting Steam IDs
 *
 * @author TimePath
 * @see <a>https://developer.valvesoftware.com/wiki/SteamID</a>
 * @see <a>http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v0001/?format=vdf&key=...</a>
 * @see <a>http://forums.alliedmods.net/showthread.php?t=60899</a>
 * @see <a>http://forums.alliedmods.net/showthread.php?p=750532</a>
 * @see <a>http://sapi.techieanalyst.net/</a>
 */
public class SteamID(public var user: String?,
                     public val ID64: String,
                     public val ID32: String,
                     public val UID: String) {

    override fun toString(): String {
        return MessageFormat.format("[{0}, {1}, {2}, {3}]", user, this.ID64, this.UID, this.ID32)
    }

    class object {

        /**
         * Steam_# 0 from HL to TF2, 1 from L4D to CS:GO
         */
        private val ID32 = Pattern.compile("STEAM_([0-9]):([0-9]):([0-9]{4,})")
        private val ID64 = Pattern.compile("([0-9]{17,})")
        private val UID = Pattern.compile("U:([0-9]):([0-9]{4,})")
        private val LOG = Logger.getLogger(javaClass<SteamID>().getName())
        /**
         * The 4 is because hexadecimal; sqrt 16? 2^4 = 16? Probably that
         */
        private val ID_64_OFFSET = BigInteger.valueOf(0x01100001).shiftLeft(8 * 4)

        public fun ID32toID64(steam: CharSequence): String? {
            return ID32toUID(steam)?.let {
                UIDtoID64(it)
            }
        }

        private fun ID32toUID(steam: CharSequence): CharSequence? {
            val matcher = ID32.matcher(steam)
            if (!matcher.matches()) {
                return null
            }
            val id = BigInteger(matcher.group(3)).multiply(BigInteger.valueOf(2)).add(BigInteger(matcher.group(2)))
            return "U:1:" + id
        }

        private fun UIDtoID64(steam: CharSequence): String? {
            val matcher = UID.matcher(steam)
            if (!matcher.matches()) {
                return null
            }
            val id = BigInteger(matcher.group(2)).add(ID_64_OFFSET)
            return id.toString()
        }

        public fun ID64toID32(steam: CharSequence): String? {
            return ID64toUID(steam)?.let {
                UIDtoID32(it)
            }
        }

        public fun ID64toUID(steam: CharSequence): String? {
            val matcher = ID64.matcher(steam)
            if (!matcher.matches()) {
                return null
            }
            val id = BigInteger(matcher.group(1)).subtract(ID_64_OFFSET)
            return "U:1:" + id
        }

        public fun UIDtoID32(steam: CharSequence): String? {
            val matcher = UID.matcher(steam)
            if (!matcher.matches()) {
                return null
            }
            val id = BigInteger(matcher.group(2)).divideAndRemainder(BigInteger.valueOf(2))
            return "STEAM_0:" + id[1] + ':' + id[0]
        }
    }
}
