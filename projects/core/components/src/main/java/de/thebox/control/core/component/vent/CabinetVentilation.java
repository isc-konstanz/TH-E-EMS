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
package de.thebox.control.core.component.vent;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.vent.CabinetTemperatureListener.CabinetTemperatureCallbacks;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;

public class CabinetVentilation implements CabinetTemperatureCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(CabinetVentilation.class);

	private double temperatureMax;
	private double temperatureTolerance;
	private final List<CabinetTemperatureListener> temperatureListeners = new ArrayList<CabinetTemperatureListener>();
	private final List<CabinetTemperature> temperatureHighFlags = new ArrayList<CabinetTemperature>(CabinetTemperature.values().length);

	private Channel state;
	private ChannelListener stateListener;

	private Value stateValueLast = null;
	private volatile long startTimeLast = 0;
	private int intervalMin;

	private volatile boolean enabled = false;

	public CabinetVentilation(ControlService control, Preferences prefs) throws ConfigurationException {
		try {
			if (prefs.nodeExists(CabinetVentilationConfig.SECTION)) {
				logger.info("Activating TH-E Control cabinet ventilation management");
				
				CabinetVentilationConfig config = new CabinetVentilationConfig(prefs);
				this.temperatureMax = config.getTemperatureMax();
				this.temperatureTolerance = config.getTemperatureTolerance();
				registerTemperatureListener(control.getChannel(config.getTopTemperatureKey()), CabinetTemperature.TOP);
				registerTemperatureListener(control.getChannel(config.getCenterTemperatureKey()), CabinetTemperature.CENTER);
				registerTemperatureListener(control.getChannel(config.getBottomTemperatureKey()), CabinetTemperature.BOTTOM);
				
				this.state = control.getChannel(config.getStateKey());
				this.stateListener = registerStateListener(state);
				this.intervalMin = config.getIntervalMin();
			}
		} catch (UnknownChannelException | BackingStoreException e) {
			throw new ConfigurationException("Invalid circulation configuration: " + e.getMessage());
		}
		this.enabled = true;
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

	public void deactivate() {
		if (stateListener != null) {
			stateListener.deregister();
		}
		for (CabinetTemperatureListener listener: temperatureListeners) {
			listener.deregister();
		}
	}

	public void disable() {
		setEnabled(false);
	}

	public void enable() {
		setEnabled(true);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void start() {
		state.write(new BooleanValue(true));
	}

	public void stop() {
		state.write(new BooleanValue(false));
	}

	@Override
	public synchronized void onTemperatureReceived(CabinetTemperature type, Double temperature) {
		if (enabled) {
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
