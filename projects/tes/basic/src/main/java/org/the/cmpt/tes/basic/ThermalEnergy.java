package org.the.cmpt.tes.basic;

import org.the.ems.core.CogeneratorService;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.HeatPumpService;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public class ThermalEnergy {

	private final ComponentService component;

	private Double energy = Double.NaN;

	public ThermalEnergy(ComponentService component) {
		this.component = component;
	}

	public ComponentService getService() {
		return component;
	}

	public Value getDelta() throws ComponentException {
		switch(component.getType()) {
		case COMBINED_HEAT_POWER:
			return calcDelta(((CogeneratorService) component).getThermalEnergy());
		case HEAT_PUMP:
			return calcDelta(((HeatPumpService) component).getThermalEnergy());
		default:
			return DoubleValue.emptyValue();
		}
	}

	private Value calcDelta(Value value) {
		double deltaEnergy = 0;
		if (!energy.isNaN()) {
			deltaEnergy = value.doubleValue() - energy;
		}
		this.energy = value.doubleValue();
		return new DoubleValue(deltaEnergy, value.getTime());
	}

}
