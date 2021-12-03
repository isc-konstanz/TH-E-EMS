package org.the.ems.cmpt.inv.effekta;

public enum Mode {

    DISABLED("00100001"),
    DEFAULT("10110110"),
    CHARGE_FROM_GRID("11111001"),
    FEED_INTO_GRID("00111111");

	private final Short mode;

	private Mode(String modeStr) {
		// TODO: implement bit parsing
		this.mode = null;
	}

    public short getMode() {
        return mode;
    }

}
