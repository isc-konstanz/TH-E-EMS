package org.the.cmpt.inv.kaco.blueplanet;

import org.osgi.service.component.annotations.Component;
import org.the.ems.core.ElectricalEnergyStorageService;
import org.the.ems.core.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.config.Configuration;

@Component(service = ElectricalEnergyStorageService.class)
public class BlueplanetBattery extends ElectricalEnergyStorage {
	private final static String ID = "KACO blueplanet battery";

	@Configuration(mandatory=false)
	private double socMin = 10;

	@Override
	public String getId() {
		return ID;
	}

	public double getMinStateOfCharge() {
		return socMin;
	}

}
