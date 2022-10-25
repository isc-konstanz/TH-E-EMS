package org.the.ems.cmpt.inv.fronius;

import org.the.ems.cmpt.ees.ElectricalEnergyStorage;


public class FroniusBattery extends ElectricalEnergyStorage {

	private final static String SECTION = "EnergyStorage";

	protected FroniusBattery() {
		super(SECTION);
	}

	@Override
	public boolean isEnabled() {
		// TODO: Implement optional battery module and remove placeholder here
		return false;
	}

}
