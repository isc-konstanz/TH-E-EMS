package org.the.ems.cmpt.inv.kaco;

import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.config.Configuration;


public class BlueplanetHyBat extends ElectricalEnergyStorage {

	private final static String SECTION = "Storage";

	protected BlueplanetHyBat() {
		super(SECTION);
	}

	@Configuration(mandatory=false)
	private int tricklePower = 0;

	public int getTricklePower() {
		return tricklePower;
	}

}
