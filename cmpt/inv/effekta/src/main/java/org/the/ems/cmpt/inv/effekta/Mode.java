package org.the.ems.cmpt.inv.effekta;

public enum Mode {

    DISABLED("00100001"),
    DEFAULT("10110110"),
    CHARGE_FROM_GRID("11111001"),
    FEED_INTO_GRID("00111111");

	private final byte bits;

	private Mode(String bitStr) {
		this(Byte.parseByte(bitStr, 2));
	}

	private Mode(byte bits) {
		this.bits = bits;
	}

	public byte getByte() {
		return bits;
	}

}
