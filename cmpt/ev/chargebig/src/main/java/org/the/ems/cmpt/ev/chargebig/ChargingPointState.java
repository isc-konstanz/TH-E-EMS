package org.the.ems.cmpt.ev.chargebig;

import java.util.HashMap;
import java.util.Map;

public enum ChargingPointState {

	FAILURE(0),
	STANDBY(1),
	CONNECTED(2),
	CHARGING(3),
	VENTILATING(4),
	ERROR(5);

	private final int code;

	private ChargingPointState(int code) {
		this.code = code;
	}

	public byte getCode() {
		return (byte) code;
	}

	static final Map<Byte, ChargingPointState> stateCodes = new HashMap<>();
	static {
		for (ChargingPointState state : ChargingPointState.values()) {
			if (stateCodes.put(state.getCode(), state) != null) {
				throw new IllegalArgumentException("Duplicate charge point state code: " + state.getCode());
			}
		}
	}

	public static ChargingPointState valueOf(int code) {
		ChargingPointState state = stateCodes.get((byte) code);
		if (state == null) {
			throw new IllegalArgumentException("Unknown charge point state code: " + code);
		}
		return state;
	}

}
