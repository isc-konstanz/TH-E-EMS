package org.the.cmpt.hp.weider;

import java.util.HashMap;
import java.util.Map;

public enum Season {
	SUMMER(0),
	WINTER(1);

	private final int code;
	private Season(int code) {
		this.code = code;
	}

    public byte getCode() {
        return (byte) code;
    }

    private static final Map<Byte, Season> codes = new HashMap<>();
    static {
        for (Season enumInstance : Season.values()) {
            if (codes.put(enumInstance.getCode(), enumInstance) != null) {
                throw new IllegalArgumentException("Duplicate code: " + enumInstance.getCode());
            }
        }
    }

    public static Season valueOf(int code) {
    	Season enumInstance = codes.get((byte) code);
        if (enumInstance == null) {
            throw new IllegalArgumentException("Unknown Season code: " + code);
        }
        return enumInstance;
    }
}
