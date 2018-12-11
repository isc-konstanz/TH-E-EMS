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
package de.thebox.control.core.component.circ.pump;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.circ.Circulation;
import de.thebox.control.core.component.circ.Circulation.CirculationCallbacks;
import de.thebox.control.core.component.circ.CirculationTemperature;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.BooleanValue;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;

public class CirculationPump implements CirculationCallbacks {

	private Circulation circulation;

	private Double referenceTemperatureMax;
	private Double referenceTemperatureMin;
	private double deltaTemperatureMin;

	private Channel state;
	private ChannelListener stateListener;
	private Value stateValueLast = new BooleanValue(false);
	private volatile long startTimeLast = 0;
	private int intervalMin;

	private volatile boolean enabled = false;

	public CirculationPump(ControlService control, Circulation circulation, Preferences prefs) throws ConfigurationException {
		try {
			if (prefs.nodeExists(CirculationPumpConfig.SECTION)) {
				CirculationPumpConfig config = new CirculationPumpConfig(prefs);
				if (!config.isEnabled()) {
					return;
				}
				this.deltaTemperatureMin = config.getDeltaTemperatureMin();
				this.referenceTemperatureMax = config.getReferenceTemperatureMax();
				this.referenceTemperatureMin = config.getReferenceTemperatureMin();

				circulation.register(this);
				circulation.registerTemperatureListener(control.getChannel(config.getReferenceTemperature()), CirculationTemperature.REF);
				this.circulation = circulation;

				this.state = control.getChannel(config.getState());
				this.stateListener = registerStateListener(state);
				this.intervalMin = config.getIntervalMin();
			}
		} catch (UnknownChannelException | BackingStoreException e) {
			throw new ConfigurationException("Invalid circulation pump configuration: " + e.getMessage());
		}
		this.enabled = true;
	}

	private ChannelListener registerStateListener(Channel channel) {
		ChannelListener stateListener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				boolean state = value.booleanValue();
				if (state && !stateValueLast.booleanValue()) {
					startTimeLast = value.getTime();
				}
				stateValueLast = value;
			}
		};
		return stateListener;
	}

	public void deactivate() {
		if (stateListener != null) {
			stateListener.deregister();
		}
		circulation.deregister();
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
	public void onTemperatureReferenceUpdated(Value delta) {
		if (enabled) {
			if (!stateValueLast.booleanValue() && delta.doubleValue() > referenceTemperatureMax) {
				start();
			}
			else if (delta.doubleValue() < referenceTemperatureMin && 
					System.currentTimeMillis() - startTimeLast >= intervalMin) {
				
				stop();
			}
		}
	}

	@Override
	public void onTemperatureDeltaUpdated(Value delta) {
		if (enabled && 
				stateValueLast.booleanValue() && delta.doubleValue() <= deltaTemperatureMin && 
				System.currentTimeMillis() - startTimeLast >= intervalMin) {
			
			stop();
		}
	}

}
