/* 
 * Copyright 2016-18 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
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
package org.the.ems.core.cmpt.circ;

import java.util.ArrayList;
import java.util.List;

import org.the.ems.core.ContentManagementService;
import org.the.ems.core.cmpt.circ.CirculationTemperatureListener.CirculationTemperatureCallbacks;
import org.the.ems.core.cmpt.circ.pump.CirculationPump;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;

public class Circulation implements CirculationTemperatureCallbacks {

	private ChannelListener flowCountListener;
	private Channel deltaTemperature;
	private Channel flowEnergy;
	private final List<Double> deltaTemperatureList = new ArrayList<Double>();
	private final List<CirculationTemperatureListener> temperatureListeners = new ArrayList<CirculationTemperatureListener>();

	private Value outTemperatureLast = DoubleValue.emptyValue();
	private Value inTemperatureLast = DoubleValue.emptyValue();
	private Double flowCounterLast = Double.NaN;
	private double flowEnergyLast = 0;

	private double flowSpecificHeat;
	private double flowDensity;

    /**
     * Interface used by {@link Circulation} to notify the {@link CirculationPump} about events
     */
    public interface CirculationCallbacks {

    	public void onTemperatureDeltaUpdated(Value delta);
    }

    /**
     * The Circulations current callback object, which is used to notify of events
     */
	private volatile CirculationCallbacks callbacks = null;

	public Circulation(ContentManagementService manager, Configurations configs) throws ConfigurationException {
		try {
			if (configs.hasSection(CirculationConfig.class)) {
				CirculationConfig config = configs.getSection(CirculationConfig.class);
				
				this.flowDensity = config.getFlowDensity();
				this.flowSpecificHeat = config.getFlowSpecificHeat();
				
				this.flowEnergy = manager.getChannel(config.getFlowEnergy());
				this.flowCountListener = registerFlowListener(manager.getChannel(config.getFlowCounter()));
				
				this.deltaTemperature = manager.getChannel(config.getDeltaTemperature());
				registerTemperatureListener(manager.getChannel(config.getInTemperature()), CirculationTemperature.IN);
				registerTemperatureListener(manager.getChannel(config.getOutTemperature()), CirculationTemperature.OUT);
			}
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid circulation configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerFlowListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				onFlowCountReceived(value);
			}
		};
		return listener;
	}

	public void registerTemperatureListener(Channel channel, CirculationTemperature type) {
		CirculationTemperatureListener listener = new CirculationTemperatureListener(this, type, channel);
		temperatureListeners.add(listener);
	}

	public void deactivate() {
		if (flowCountListener != null) {
			flowCountListener.deregister();
		}
		for (CirculationTemperatureListener listener: temperatureListeners) {
			listener.deregister();
		}
	}

	public void register(CirculationCallbacks callbacks) {
		this.callbacks = callbacks;
	}

	public void deregister() {
		this.callbacks = null;
	}

	private void onFlowCountReceived(Value counter) {
		if (!flowCounterLast.isNaN()) {
			// Flow since last calculation in kilogramm
			double flowMass = (counter.doubleValue() - flowCounterLast)*flowDensity;
			
			double tempDelta;
			if (deltaTemperatureList.size() > 0) {
				tempDelta = 0;
				for (double temp : deltaTemperatureList) {
					tempDelta += temp;
				}
				tempDelta /= deltaTemperatureList.size();
				deltaTemperatureList.clear();
			}
			else {
				tempDelta = deltaTemperature.getLatestValue().doubleValue();
			}
			// Calculate energy in kJ = cp*m*dT
			double energy = flowSpecificHeat*flowMass*tempDelta;
			flowEnergyLast += energy/3600;
			flowEnergy.setLatestValue(new DoubleValue(flowEnergyLast, counter.getTime()));
		}
		flowCounterLast = counter.doubleValue();
	}

	@Override
	public synchronized void onTemperatureReceived(CirculationTemperature type, Value temperature) {
		switch(type) {
		case IN:
			inTemperatureLast = temperature;
			break;
		case OUT:
			outTemperatureLast = temperature;
			break;
		}
		if (type == CirculationTemperature.OUT || type == CirculationTemperature.IN) {
			if (outTemperatureLast.getTime() == inTemperatureLast.getTime()) {
				
				double delta = outTemperatureLast.doubleValue() - inTemperatureLast.doubleValue();
				Value value = new DoubleValue(delta, outTemperatureLast.getTime());
				
				deltaTemperature.setLatestValue(value);
				deltaTemperatureList.add(delta);
				if (callbacks != null) {
					callbacks.onTemperatureDeltaUpdated(value);
				}
			}
		}
	}
}
