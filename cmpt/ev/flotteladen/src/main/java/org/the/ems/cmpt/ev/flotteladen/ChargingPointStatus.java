package org.the.ems.cmpt.ev.flotteladen;

import java.util.HashMap;
import java.util.Map;

import org.the.ems.core.data.Value;

public enum ChargingPointStatus {

	STANDBY(0),
	AVAILABLE(1),
	CONNECTED(2),
	CHARGING_STARTED_EV(3),
	CHARGING(4),
	CHARGING_COMPLETE(5);

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

	public static ChargingPointStatus valueOf(Value value) {
		String status = value.stringValue().trim().replace("\"", "");
		return ChargingPointStatus.valueOf(status);
	}

	public static ChargingPointStatus valueOf(int code) {
		ChargingPointStatus status = stateCodes.get((byte) code);
		if (status == null) {
			throw new IllegalArgumentException("Unknown charging point status code: " + code);
		}
		return status;
	}

}
