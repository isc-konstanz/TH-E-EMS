package org.the.cmpt.hp.weider;

public enum ChargingMode {
	HOT_WATER,
	WARM_WATER;

	@Override
	public String toString() {
		switch (this) {
		case HOT_WATER:
			return "Hot Water";
		case WARM_WATER:
		default:
			return "Warm Water";
		}
	}
}
