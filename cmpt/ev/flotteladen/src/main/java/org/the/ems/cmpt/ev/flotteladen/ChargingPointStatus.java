package org.the.ems.cmpt.ev.flotteladen;

import java.util.HashMap;
import java.util.Map;

public enum ChargingPointStatus {

	AVAILABLE(0),
	CONNECTED(1),
	CHARGING(2),
	CHARGING_COMPLETE(3);

	private final int code;

	private ChargingPointStatus(int code) {
		this.code = code;
	}

	public byte getCode() {
		return (byte) code;
	}

	static final Map<Byte, ChargingPointStatus> stateCodes = new HashMap<>();
	static {
		for (ChargingPointStatus state : ChargingPointStatus.values()) {
			if (stateCodes.put(state.getCode(), state) != null) {
				throw new IllegalArgumentException("Duplicate charging point status code: " + state.getCode());
			}
		}
	}

	public static ChargingPointStatus valueOf(int code) {
		ChargingPointStatus state = stateCodes.get((byte) code);
		if (state == null) {
			throw new IllegalArgumentException("Unknown charging point status code: " + code);
		}
		return state;
	}

}
