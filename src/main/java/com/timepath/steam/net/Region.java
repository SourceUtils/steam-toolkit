package com.timepath.steam.net;

/**
 * @author TimePath
 */
public enum Region {
    ALL(255),
    US_EAST(0),
    US_WEST(1),
    SOUTH_AMERICA(2),
    EUROPE(3),
    ASIA(4),
    AUSTRALIA(5),
    MIDDLE_EAST(6),
    AFRICA(7);
    private final byte code;

    Region(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }
}
