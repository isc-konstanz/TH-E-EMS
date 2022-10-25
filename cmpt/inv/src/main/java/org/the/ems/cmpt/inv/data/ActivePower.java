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
package org.the.ems.cmpt.inv.data;

import org.the.ems.cmpt.inv.InverterCallbacks;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;

public class ActivePower extends Component implements Channel, ValueListener {

	private final static String SECTION = "External";

	private volatile InverterCallbacks callbacks = null;

	@Configuration(value="power_active", section=Configurations.GENERAL)
	private Channel activePower;

	@Configuration(value="power_source")
	private Channel sourcePower;

	@Configuration(value="power_extern")
	private Channel externalPower;

	@Configuration(value="scaling_external", mandatory=false)
	private double externalScaling = -1;

	private volatile boolean running = false;

	public ActivePower() {
		super(SECTION);
	}

	@Override
	protected void onActivate() throws ComponentException {
		super.onActivate();
		if (isEnabled()) {
			sourcePower.registerValueListener(new ActivePowerListener());
		}
		activePower.registerValueListener(this);
		
		running = true;
	}

	public void register(InverterCallbacks callbacks) {
		this.callbacks = callbacks;
	}

	public void resume() {
		running = true;
	}

	public void pause() {
		running = false;
	}

	public void deregister() {
		this.callbacks = null;
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		this.running = false;
	}

	public Value getLatestValue() throws InvalidValueException {
		return activePower.getLatestValue();
	}

	@Override
	public Value getLatestValue(ValueListener listener) throws InvalidValueException {
		return activePower.getLatestValue(listener);
	}

	@Override
	public void registerValueListener(ValueListener listener) {
		activePower.registerValueListener(listener);
	}

	@Override
	public void deregisterValueListener(ValueListener listener) {
		activePower.deregisterValueListener(listener);
	}

	@Override
	public void deregisterValueListeners() {
		activePower.deregisterValueListeners();
	}

	@Override
	public void setLatestValue(Value value) {
		activePower.setLatestValue(value);
	}

	@Override
	public void write(ValueList values) throws EnergyManagementException {
		activePower.write(values);
	}

	@Override
	public void write(Value value) throws EnergyManagementException {
		activePower.write(value);
	}

	public boolean isRunning() {
		return isEnabled() && running;
	}

	public double getExternalPower() {
		Value value;
		try {
			value = externalPower.getLatestValue();
			
		} catch (InvalidValueException e) {
			value = DoubleValue.zeroValue();
		}
		return externalScaling*value.doubleValue();
	}

	@Override
	public void onValueReceived(Value value) {
		// Always notify callback of received consumption values, as consumption may be a generic sampled channel
		if (callbacks != null) {
			callbacks.onSetpointUpdate();
		}
	}

	private class ActivePowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (isRunning()) {
				double activePowerValue = value.doubleValue() + getExternalPower();
				activePower.setLatestValue(new DoubleValue(activePowerValue, value.getEpochMillis()));
			}
		}
	}

}
