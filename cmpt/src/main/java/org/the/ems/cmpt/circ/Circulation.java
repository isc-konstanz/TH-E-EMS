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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.circ.FlowTemperatureListener.CirculationTemperatureCallbacks;
import org.the.ems.cmpt.util.PowerListener;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;

public class Circulation extends Component implements CirculationTemperatureCallbacks {

	private static final Logger logger = LoggerFactory.getLogger(Circulation.class);

	public final static String SECTION = "Circulation";

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
	@Configuration(mandatory = false)
	private Channel flowCounter;

	// The channel key of the volume in liter hours
	@Configuration(mandatory = false)
	private Channel flowVolume;

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

	private List<Value> flowTempDeltaValues = new ArrayList<Value>();

	private DoubleValue flowTempInLast = DoubleValue.emptyValue();
	private DoubleValue flowTempOutLast = DoubleValue.emptyValue();

	public Circulation() {
		super(SECTION);
	}

	public Value getFlowInletTemperature() throws InvalidValueException {
		if (!flowTempInLast.isNaN()) {
			return flowTempInLast;
		}
		return flowTempIn.getLatestValue();
	}

	private void setFlowInletTemperature(Value temperature) {
		if (temperature instanceof DoubleValue) {
			flowTempInLast = (DoubleValue) temperature;
		}
		else {
			flowTempInLast = DoubleValue.copy(temperature);
		}
	}

	public Value getFlowOutletTemperature() throws InvalidValueException {
		if (!flowTempOutLast.isNaN()) {
			return flowTempOutLast;
		}
		return flowTempOut.getLatestValue();
	}

	private void setFlowOutletTemperature(Value temperature) {
		if (temperature instanceof DoubleValue) {
			flowTempOutLast = (DoubleValue) temperature;
		}
		else {
			flowTempOutLast = DoubleValue.copy(temperature);
		}
	}

	public Value getFlowDeltaTemperature() throws InvalidValueException {
		return flowTempDelta.getLatestValue();
	}

	@Override
	protected void onActivate() throws ComponentException {
		super.onActivate();
		if (!isEnabled()) {
			return;
		}
		if (flowVolume != null) {
			flowVolume.registerValueListener(new FlowVolumeListener(flowEnergy));
		}
		else if (flowCounter != null) {
			flowCounter.registerValueListener(new FlowCountListener(flowEnergy));
		}
		else {
			throw new ConfigurationException("Missing configured flow volume or counter");
		}
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
	public synchronized void onTemperatureReceived(FlowTemperature type, Value temperature) {
		switch(type) {
		case IN:
			setFlowInletTemperature(temperature);
			break;
		case OUT:
			setFlowOutletTemperature(temperature);
			break;
		default:
			return;
		}
		if (!flowTempInLast.isNaN() && !flowTempOutLast.isNaN() && 
				Math.abs(flowTempOutLast.getEpochMillis() - flowTempInLast.getEpochMillis()) < 1000) {
			double delta = flowTempOutLast.doubleValue() - flowTempInLast.doubleValue();
			Value value = new DoubleValue(delta, System.currentTimeMillis());
			
			synchronized (flowTempDeltaValues) {
				flowTempDelta.setLatestValue(value);
				flowTempDeltaValues.add(value);
				if (callbacks != null) {
					callbacks.onTemperatureDeltaUpdated(value);
				}
			}
		}
	}

	private class FlowListener extends PowerListener {

		public FlowListener(Channel energy) {
			super(energy);
		}

		protected void onLitersReceived(double flow, long timestamp) {
			logger.debug("Received {}l water flowing in circulation", flow);

			// Flow since last calculation in kilogram
			double flowMass = flow*flowDensity;

			Double flowTempDeltaValue = Double.NaN;
			synchronized (flowTempDeltaValues) {
				try {
					if (flowTempDeltaValues.size() > 0) {
						flowTempDeltaValue = flowTempDeltaValues.stream()
								.mapToDouble(d -> d.doubleValue())
								.average().orElse(Double.NaN);
						
						flowTempDeltaValues.clear();
					}
					else {
						flowTempDeltaValue = flowTempDelta.getLatestValue().doubleValue();
					}
				} catch (InvalidValueException e) {
					// Do nothing, as value is already NaN
				}
				if (flowTempDeltaValue.isNaN()) {
					flowTempDeltaValues.clear();
					return;
				}
			}
			// Calculate energy in Q[kJ] = cp*m[kg]*dT[°C]
			double flowEnergyValue = flowSpecificHeat*flowMass*flowTempDeltaValue;

			if (logger.isDebugEnabled()) {
				logger.debug("Water flow with delta temperature of {}°C circulated {} kWh", 
						String.format("%.2f", flowTempDeltaValue), 
						String.format("%.2f", flowEnergyValue));
			}
			if (energyLatest != null) {
				// Calculate average power since last counter tick
				long timeDelta = (timestamp - energyLatest.getEpochMillis())/1000;
				double flowPowerValue = flowEnergyValue/timeDelta;
				flowPower.setLatestValue(new DoubleValue(flowPowerValue*1000, timestamp));

				if (logger.isDebugEnabled()) {
					logger.debug("Water flow represents {}W power", 
							String.format("%.2f", flowPowerValue*1000));
				}
			}
			onEnergyReceived(new DoubleValue(flowEnergyValue/3600, timestamp));
		}
	}

	private class FlowVolumeListener extends FlowListener {

		private long flowTimeLast = -1;

		public FlowVolumeListener(Channel energy) {
			super(energy);
		}

		@Override
		public void onValueReceived(Value volume) {
			long flowTime = volume.getEpochMillis();
			if (flowTimeLast > 0) {
				long flowTimeDelta = flowTime - flowTimeLast;
				// Flow since last calculation in liters
				double flow = volume.doubleValue()*((double) flowTimeDelta/3600000);
				onLitersReceived(flow, flowTime);
			}
			flowTimeLast = flowTime;
		}
	}

	private class FlowCountListener extends FlowListener {

		private Double flowCounterLast = Double.NaN;

		public FlowCountListener(Channel energy) {
			super(energy);
		}

		@Override
		public void onValueReceived(Value counter) {
			long flowTime = counter.getEpochMillis();
			if (!flowCounterLast.isNaN()) {
				// Flow since last calculation in liters
				double flow = counter.doubleValue() - flowCounterLast;
				onLitersReceived(flow, flowTime);
			}
			flowCounterLast = counter.doubleValue();
		}
	}

}
