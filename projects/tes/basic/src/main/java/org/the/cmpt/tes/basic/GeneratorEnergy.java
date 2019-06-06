package org.the.cmpt.tes.basic;

import org.the.ems.core.ComponentException;
import org.the.ems.core.GeneratorService;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public class GeneratorEnergy {

	private final GeneratorService component;

	private Double energy = Double.NaN;

	public GeneratorEnergy(GeneratorService component) {
		this.component = component;
	}

	public GeneratorService getService() {
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
