package org.the.ems.cmpt.tes;

import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public class GeneratorEnergy {

	private final HeatingService component;

	private Double energy = Double.NaN;

	public GeneratorEnergy(HeatingService component) {
		this.component = component;
	}

	public HeatingService getService() {
		return component;
	}

	public Value getValue() throws ComponentException {
		Value value = component.getThermalEnergy();
		double energyDelta = 0;
		if (!energy.isNaN()) {
			energyDelta = value.doubleValue() - energy;
		}
		energy = value.doubleValue();
		
		return new DoubleValue(energyDelta, value.getTime());
	}

}
