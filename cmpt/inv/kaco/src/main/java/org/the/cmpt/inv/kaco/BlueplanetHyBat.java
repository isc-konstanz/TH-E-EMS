package org.the.cmpt.inv.kaco;

import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.config.Configuration;


public class BlueplanetHyBat extends ElectricalEnergyStorage {

	@Configuration(mandatory=false)
	private int tricklePower = 0;

	public int getTricklePower() {
		return tricklePower;
	}

}
