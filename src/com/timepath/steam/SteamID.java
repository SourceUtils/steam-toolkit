package com.timepath.steam;

import java.math.BigInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * https://developer.valvesoftware.com/wiki/SteamID
 * http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v0001/?key=303E8E7C12216D62FD8F522602CE141C&format=vdf
 * http://forums.alliedmods.net/showthread.php?t=60899
 * http://forums.alliedmods.net/showthread.php?p=750532
 * http://sapi.techieanalyst.net/
 *
 * @author TimePath
 */
public class SteamID {

    /**
     * Steam_# 0 from HL to TF2, 1 from L4D to CS:GO
     */
    public static final Pattern ID32 = Pattern.compile("STEAM_([0-9]):([0-9]):([0-9]{4,})");

    /**
     * http://steamcommunity.com/profiles/[uid]
     */
    public static final Pattern UID = Pattern.compile("U:([0-9]):([0-9]{4,})");

    /**
     * http://steamcommunity.com/profiles/id64
     */
    public static final Pattern ID64 = Pattern.compile("([0-9]{17,})");

    /**
     * The 4 is because hexadecimal; sqrt 16? 2^4 = 16? Probably that
     */
    private static final BigInteger id64Offset = BigInteger.valueOf(0x01100001).shiftLeft(8 * 4);

    private static final Logger LOG = Logger.getLogger(SteamID.class.getName());

    public static String ID32toID64(String steam) {
        return UIDtoID64(ID32toUID(steam));
    }

    public static String ID32toUID(String steam) {
        Matcher m = ID32.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(m.group(3)).multiply(BigInteger.valueOf(2)).add(
            new BigInteger(m.group(2)));
        return "U:1:" + id.toString();
    }

    public static String UIDtoID32(String steam) {
        Matcher m = UID.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger[] id = new BigInteger(m.group(2)).divideAndRemainder(BigInteger.valueOf(2));
        return "STEAM_0:" + id[1] + ":" + id[0];
    }

    public static String UIDtoID64(String steam) {
        Matcher m = UID.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(m.group(2)).add(id64Offset);
        return id.toString();
    }

    public static String ID64toUID(String steam) {
        Matcher m = ID64.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(m.group(1)).subtract(id64Offset);
        return "U:1:" + id.toString();
    }

    public static String ID64toID32(String steam) {
        return UIDtoID32(ID64toUID(steam));
    }

    private String user;

    private final String id64, uid, id32;

    public SteamID(String user, String id64, String uid, String id32) {
        this.user = user;
        this.id64 = id64;
        this.uid = uid;
        this.id32 = id32;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public String getUID() {
        return uid;
    }

    public String getID64() {
        return id64;
    }

    public String getID32() {
        return id32;
    }

    @Override
    public String toString() {
        return "[" + user + ", " + id64 + ", " + uid + ", " + id32 + "]";
    }

}
