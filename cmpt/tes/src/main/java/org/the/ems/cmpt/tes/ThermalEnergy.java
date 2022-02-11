package org.the.ems.cmpt.tes;

import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;

public class ThermalEnergy {

	private final HeatingService component;

	private Double energyLatest = Double.NaN;

	public ThermalEnergy(HeatingService component) {
		this.component = component;
	}

	public String getHeatingId() {
		return component.getId();
	}

	public HeatingService getHeating() {
		return component;
	}

	public Value getValue() throws ComponentException, InvalidValueException {
		Value energy = component.getThermalEnergy();
		double energyDelta = 0;
		if (!energyLatest.isNaN()) {
			energyDelta = Math.max(energy.doubleValue() - energyLatest, 0);
		}
		energyLatest = energy.doubleValue();
		
		return new DoubleValue(energyDelta, energy.getEpochMillis());
	}

}
