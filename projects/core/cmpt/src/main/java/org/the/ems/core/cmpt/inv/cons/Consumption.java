/* 
 * Copyright 2016-19 ISC Konstanz
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
package org.the.ems.core.cmpt.inv.cons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.the.ems.core.ContentManagementService;
import org.the.ems.core.cmpt.inv.InverterCallbacks;
import org.the.ems.core.cmpt.inv.InverterConfig;
import org.the.ems.core.cmpt.inv.cons.PowerListener.PowerCallbacks;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.UnknownChannelException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class Consumption implements PowerCallbacks {

	protected final InverterCallbacks callbacks;

	private Channel consumption;
	protected Map<PowerType, Value> powerValues;
	protected List<PowerListener> powerListeners;
	protected final ConsumptionListener listener;

	protected class ConsumptionListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			callbacks.onSetpointUpdate();
		}
	}

	public Consumption(InverterCallbacks callbacks, ContentManagementService control, InverterConfig inverter, Configurations configs)
			throws ConfigurationException {
		this.callbacks = callbacks;
		this.listener = new ConsumptionListener();
		try {
			this.consumption = control.getChannel(inverter.getConsumptionPower());
			this.consumption.registerValueListener(listener);
			
			if (configs.hasSection(ConsumptionConfig.class)) {
				ConsumptionConfig config = configs.getSection(ConsumptionConfig.class);
				
				powerValues = new HashMap<PowerType, Value>();
				powerListeners = new ArrayList<PowerListener>();
				registerPowerValueListener(control.getChannel(config.getStoragePower()), PowerType.ESS);
				registerPowerValueListener(control.getChannel(config.getAcPower()), PowerType.AC);
				if (!config.hasDcPower2()) {
					registerPowerValueListener(control.getChannel(config.getDcPower()), PowerType.DC1);
				}
				else {
					registerPowerValueListener(control.getChannel(config.getDcPower1()), PowerType.DC1);
					registerPowerValueListener(control.getChannel(config.getDcPower2()), PowerType.DC2);
				}
			}
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid consumption configuration: " + e.getMessage());
		}
	}

	private void registerPowerValueListener(Channel channel, PowerType type) {
		powerValues.put(type, DoubleValue.emptyValue());
		
		PowerListener listener = new PowerListener(this, type, channel);
		powerListeners.add(listener);
	}

	public void deactivate() {
		for (PowerListener listener: powerListeners) {
			listener.deregister();
		}
		consumption.deregisterValueListener(listener);
	}

	public Value getLatestValue() {
		return consumption.getLatestValue();
	}

	@Override
	public void onPowerValueReceived(PowerType type, Value power) {
		long time = power.getTime();
		
		powerValues.put(type, power);
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
		consumption -= powerValues.get(PowerType.ESS).doubleValue();
		
		if (consumption < 0) {
			consumption = 0;
		}
		this.consumption.setLatestValue(new DoubleValue(consumption, time));
	}

}
