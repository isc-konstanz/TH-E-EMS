/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.the.ems.cmpt.circ;

import java.util.ArrayList;
import java.util.List;

import org.the.ems.cmpt.circ.FlowTemperatureListener.CirculationTemperatureCallbacks;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class Circulation extends Component implements CirculationTemperatureCallbacks {

	private final static String SECTION = "Circulation";

	/**
	 * Interface used by {@link Circulation} to notify the {@link CirculationPump} about events
	 */
	protected interface CirculationCallbacks {

		public void onTemperatureDeltaUpdated(Value delta);
	}

	/**
	 * The Circulations current callback object, which is used to notify of events
	 */
	private volatile CirculationCallbacks callbacks = null;

	// The specific heat capacity of the flow medium. Default is 4.1813 of water.
	@Configuration(mandatory=false)
	private double flowSpecificHeat = 4.1813;

	// The density of the flow medium. Default is 1 of water.
	@Configuration(mandatory=false)
	private double flowDensity = 1;

	// The channel key of the counter in liters
	@Configuration
	private Channel flowCounter;

	@Configuration
	private Channel flowEnergy;

	@Configuration
	private Channel flowPower;

	@Configuration
	private Channel flowTempIn;

	@Configuration
	private Channel flowTempOut;

	@Configuration
	private Channel flowTempDelta;

	private List<Double> flowTempValues = new ArrayList<Double>();

	private Value flowTempInLast = DoubleValue.emptyValue();
	private Value flowTempOutLast = DoubleValue.emptyValue();
	private Value flowEnergyLast = DoubleValue.emptyValue();
	private Double flowCounterLast = Double.NaN;

	public Circulation() {
		super(SECTION);
	}

	@Override
	protected void onActivate() throws ComponentException {
		super.onActivate();
		if (!isEnabled()) {
			return;
		}
		flowCounter.registerValueListener(new FlowCountListener());
		flowTempIn.registerValueListener(new FlowTemperatureListener(this, FlowTemperature.IN));
		flowTempOut.registerValueListener(new FlowTemperatureListener(this, FlowTemperature.OUT));
	}

	public void register(CirculationCallbacks callbacks) {
		this.callbacks = callbacks;
	}

	public void deregister() {
		this.callbacks = null;
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		this.deregisterConfiguredValueListeners();
	}

	@Override
	public synchronized void onTemperatureReceived(FlowTemperature type, Value temperature) {
		switch(type) {
		case IN:
			flowTempInLast = temperature;
			break;
		case OUT:
			flowTempOutLast = temperature;
			break;
		}
		if (type == FlowTemperature.OUT || type == FlowTemperature.IN) {
			if (flowTempOutLast.getEpochMillis() == flowTempInLast.getEpochMillis()) {
				
				double delta = flowTempOutLast.doubleValue() - flowTempInLast.doubleValue();
				Value value = new DoubleValue(delta, flowTempOutLast.getEpochMillis());
				
				flowTempDelta.setLatestValue(value);
				flowTempValues.add(delta);
				if (callbacks != null) {
					callbacks.onTemperatureDeltaUpdated(value);
				}
			}
		}
	}

	private class FlowCountListener implements ValueListener {

		@Override
		public void onValueReceived(Value counter) {
			if (!flowCounterLast.isNaN()) {
				// Flow since last calculation in kilogram
				double flowMass = (counter.doubleValue() - flowCounterLast)*flowDensity;
				
				double tempDelta;
				if (flowTempValues.size() > 0) {
					tempDelta = 0;
					for (double temp : flowTempValues) {
						tempDelta += temp;
					}
					tempDelta /= flowTempValues.size();
					flowTempValues.clear();
				}
				else {
					try {
						tempDelta = flowTempDelta.getLatestValue().doubleValue();
						
					} catch (InvalidValueException e) {
						tempDelta = 0;
					}
				}
				// Calculate energy in Q[kJ] = cp*m[kg]*dT[�C]
				double energy = flowSpecificHeat*flowMass*tempDelta;
				
				// Calculate average power since last counter tick
				long timeDelta = (counter.getEpochMillis() - flowEnergyLast.getEpochMillis())/1000;
				double power = energy/timeDelta;
				flowPower.setLatestValue(new DoubleValue(power, counter.getEpochMillis()));
				
				double energyTotal = flowEnergyLast.doubleValue() + energy/3600;
				flowEnergyLast = new DoubleValue(energyTotal, counter.getEpochMillis());
				flowEnergy.setLatestValue(flowEnergyLast);
			}
			flowCounterLast = counter.doubleValue();
		}
	}

}
