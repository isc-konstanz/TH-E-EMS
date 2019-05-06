package org.the.cmpt.tes.basic;

import org.osgi.service.component.annotations.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ThermalEnergyStorageService;
import org.the.ems.core.cmpt.ConfiguredComponent;
import org.the.ems.core.data.Value;

@Component
public class ThermalEnergyStorage extends ConfiguredComponent implements ThermalEnergyStorageService {
	private final static String ID = "Basic";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public double getCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Value getThermalPower() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value getTemperature() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

}
