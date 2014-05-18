package com.timepath.steam;

import java.math.BigInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
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
    private static final Pattern    ID32         = Pattern.compile("STEAM_([0-9]):([0-9]):([0-9]{4,})");
    /**
     * http://steamcommunity.com/profiles/id64
     */
    private static final Pattern    ID64         = Pattern.compile("([0-9]{17,})");
    /**
     * http://steamcommunity.com/profiles/[uid]
     */
    private static final Pattern    UID          = Pattern.compile("U:([0-9]):([0-9]{4,})");
    private static final Logger     LOG          = Logger.getLogger(SteamID.class.getName());
    /**
     * The 4 is because hexadecimal; sqrt 16? 2^4 = 16? Probably that
     */
    private static final BigInteger ID_64_OFFSET = BigInteger.valueOf(0x01100001).shiftLeft(8 * 4);
    private final String id32, id64, uid;
    private String user;

    public SteamID(String user, String id64, String uid, String id32) {
        this.user = user;
        this.id64 = id64;
        this.uid = uid;
        this.id32 = id32;
    }

    public static String ID32toID64(CharSequence steam) {
        return UIDtoID64(ID32toUID(steam));
    }

    private static CharSequence ID32toUID(CharSequence steam) {
        Matcher matcher = ID32.matcher(steam);
        if(!matcher.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(matcher.group(3)).multiply(BigInteger.valueOf(2)).add(new BigInteger(matcher.group(2)));
        return "U:1:" + id;
    }

    private static String UIDtoID64(CharSequence steam) {
        Matcher matcher = UID.matcher(steam);
        if(!matcher.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(matcher.group(2)).add(ID_64_OFFSET);
        return id.toString();
    }

    public static String ID64toID32(CharSequence steam) {
        return UIDtoID32(ID64toUID(steam));
    }

    public static String ID64toUID(CharSequence steam) {
        Matcher matcher = ID64.matcher(steam);
        if(!matcher.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(matcher.group(1)).subtract(ID_64_OFFSET);
        return "U:1:" + id;
    }

    public static String UIDtoID32(CharSequence steam) {
        Matcher matcher = UID.matcher(steam);
        if(!matcher.matches()) {
            return null;
        }
        BigInteger[] id = new BigInteger(matcher.group(2)).divideAndRemainder(BigInteger.valueOf(2));
        return "STEAM_0:" + id[1] + ':' + id[0];
    }

    public String getID32() {
        return id32;
    }

    public String getID64() {
        return id64;
    }

    public String getUID() {
        return uid;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return '[' + user + ", " + id64 + ", " + uid + ", " + id32 + ']';
    }
}