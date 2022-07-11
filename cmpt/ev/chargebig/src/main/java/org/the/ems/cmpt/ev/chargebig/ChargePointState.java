package org.the.ems.cmpt.ev.chargebig;

import java.util.HashMap;
import java.util.Map;

public enum ChargePointState {

	FAILURE(0),
	STANDBY(1),
	CONNECTED(2),
	CHARGING(3),
	VENTILATING(4),
	ERROR(5);

	private final int code;

	private ChargePointState(int code) {
		this.code = code;
	}

	public byte getCode() {
		return (byte) code;
	}

	static final Map<Byte, ChargePointState> stateCodes = new HashMap<>();
	static {
		for (ChargePointState state : ChargePointState.values()) {
			if (stateCodes.put(state.getCode(), state) != null) {
				throw new IllegalArgumentException("Duplicate charge point state code: " + state.getCode());
			}
		}
	}

	public static ChargePointState valueOf(int code) {
		ChargePointState state = stateCodes.get((byte) code);
		if (state == null) {
			throw new IllegalArgumentException("Unknown charge point state code: " + code);
		}
		return state;
	}

}
