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
package org.the.cmpt.vnt.basic;

import java.util.ArrayList;
import java.util.List;

import org.the.cmpt.vnt.basic.CabinetTemperatureListener.CabinetTemperatureCallbacks;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.VentilationService;
import org.the.ems.core.cmpt.GenericComponent;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;

public abstract class BasicComponent extends GenericComponent implements VentilationService, CabinetTemperatureCallbacks {

	private double temperatureMax;
	private double temperatureTolerance;
	private final List<CabinetTemperatureListener> temperatureListeners = new ArrayList<CabinetTemperatureListener>();
	private final List<CabinetTemperature> temperatureHighFlags = new ArrayList<CabinetTemperature>(CabinetTemperature.values().length);

	private Channel state;
	private ChannelListener stateListener;

	private Value stateValueLast = null;
	private volatile long startTimeLast = 0;
	private int intervalMin;

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		try {
			if (configs.hasSection(BasicConfig.class)) {
				BasicConfig config = configs.getSection(BasicConfig.class);
				this.temperatureMax = config.getTemperatureMax();
				this.temperatureTolerance = config.getTemperatureTolerance();
				registerTemperatureListener(manager.getChannel(config.getTopTemperatureKey()), CabinetTemperature.TOP);
				registerTemperatureListener(manager.getChannel(config.getCenterTemperatureKey()), CabinetTemperature.CENTER);
				registerTemperatureListener(manager.getChannel(config.getBottomTemperatureKey()), CabinetTemperature.BOTTOM);
				
				this.state = manager.getChannel(config.getStateKey());
				this.stateListener = registerStateListener(state);
				this.intervalMin = config.getIntervalMin();
			}
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid ventilation configuration: " + e.getMessage());
		}
	}

	@Override
	public void onDeactivate() {
		if (stateListener != null) {
			stateListener.deregister();
		}
		for (CabinetTemperatureListener listener: temperatureListeners) {
			listener.deregister();
		}	
	}

	private ChannelListener registerStateListener(Channel channel) {
		ChannelListener stateListener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (stateValueLast != null) {
					boolean state = value.booleanValue();
					if (state && !stateValueLast.booleanValue()) {
						startTimeLast = value.getTime();
					}
					else if (!state && temperatureHighFlags.size() > 0) {
						temperatureHighFlags.clear();
					}
				}
				stateValueLast = value;
			}
		};
		return stateListener;
	}

	private void registerTemperatureListener(Channel channel, CabinetTemperature type) {
		CabinetTemperatureListener listener = new CabinetTemperatureListener(this, type, channel);
		temperatureListeners.add(listener);
	}

	public void start() {
		state.write(new BooleanValue(true));
	}

	public void stop() {
		state.write(new BooleanValue(false));
	}

	@Override
	public synchronized void onTemperatureReceived(CabinetTemperature type, Double temperature) {
		if (!isMaintenance()) {
			if (temperature > temperatureMax + temperatureTolerance && !temperatureHighFlags.contains(type)) {
				temperatureHighFlags.add(type);
				start();
			}
			else if (temperature < temperatureMax - temperatureTolerance &&
					System.currentTimeMillis() - startTimeLast >= intervalMin) {
				
				if (stateValueLast != null && stateValueLast.booleanValue()) {
					temperatureHighFlags.remove(type);
					if (temperatureHighFlags.size() == 0) {
						stop();
					}
				}
			}
		}
	}
}
