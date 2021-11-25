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
package org.the.ems.cmpt.inv.ext;

import java.util.HashMap;
import java.util.Map;

import org.the.ems.cmpt.inv.InverterCallbacks;
import org.the.ems.cmpt.inv.ext.PowerListener.PowerCallbacks;
import org.the.ems.core.Configurable;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class ConsumptionPower extends Configurable implements PowerCallbacks, ValueListener {

	private final static String SECTION = "Consumption";

	private volatile InverterCallbacks callbacks = null;

	private ContentManagementService content;

	@Configuration(section=Configurations.GENERAL)
	private Channel consPower;

	@Configuration
	private Channel acPower;

	@Configuration(value= {"dc1_power", "dc_power"})
	private Channel dc1Power;

	@Configuration(mandatory=false)
	private Channel dc2Power;

	@Configuration
	private Channel eesPower;

	private Map<PowerType, Value> powerValues = new HashMap<PowerType, Value>();

	private volatile boolean running = false;

	public ConsumptionPower() {
		setConfiguredSection(SECTION);
	}

	@Override
	protected final ContentManagementService getContentManagement() {
		return content;
	}

	public ConsumptionPower activate(ContentManagementService content, Configurations configs) 
			throws ConfigurationException {
		
		if (configs.isEnabled(SECTION)) {
			configure(configs);
			
			consPower.registerValueListener(this);
			
			eesPower.registerValueListener(new PowerListener(this, PowerType.EES));
			acPower.registerValueListener(new PowerListener(this, PowerType.AC));
			dc1Power.registerValueListener(new PowerListener(this, PowerType.DC1));
			if (dc2Power != null) {
				dc2Power.registerValueListener(new PowerListener(this, PowerType.DC2));
			}
			running = true;
		}
		return this;
	}

	public ConsumptionPower register(InverterCallbacks callbacks) {
		this.callbacks = callbacks;
		return this;
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

	public void deactivate() {
		consPower.deregisterValueListeners();
		
		if (isEnabled()) {
			eesPower.deregisterValueListeners();
			acPower.deregisterValueListeners();
			dc1Power.deregisterValueListeners();
			if (dc2Power != null) {
				dc2Power.deregisterValueListeners();
			}
		}
		callbacks = null;
		running = false;
	}

	public Value getLatestValue() throws InvalidValueException {
		return consPower.getLatestValue();
	}

	public boolean isRunning() {
		return isEnabled() && running;
	}

	@Override
	public void onPowerValueReceived(PowerType type, Value power) {
		long time = power.getTime();
		
		powerValues.put(type, power);
		if (!isRunning()) {
			return;
		}
		for (Value value : powerValues.values()) {
			if (value.getTime() != time) {
				return;
			}
		}
		double consumption = powerValues.get(PowerType.AC).doubleValue();
		if (consumption > 0) {
			consumption += powerValues.get(PowerType.DC1).doubleValue();
			if (powerValues.containsKey(PowerType.DC2)) {
				consumption += powerValues.get(PowerType.DC2).doubleValue();
			}
		}
		consumption -= powerValues.get(PowerType.EES).doubleValue();
		
		if (consumption < 0) {
			consumption = 0;
		}
		this.consPower.setLatestValue(new DoubleValue(consumption, time));
	}

	@Override
	public void onValueReceived(Value value) {
		if (callbacks != null) {
			callbacks.onSetpointUpdate();
		}
	}

}
