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
package org.the.cmpt.vnt.basic;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.the.cmpt.vnt.basic.TemperatureListener.TemperatureCallbacks;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.ConfiguredComponent;
import org.the.ems.core.cmpt.VentilationService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

@Component(service = VentilationService.class)
public abstract class CabinetVentilation extends ConfiguredComponent 
		implements VentilationService, TemperatureCallbacks {

	@Configuration(mandatory=false, scale=60000) // Default interval minimum of 10 minutes
	private int intervalMin = 600000;

	@Configuration(mandatory=false, value="temp_tolerance")
	private double temperatureTolerance = 0.5;

	@Configuration(mandatory=false, value="temp_max")
	private double temperatureMax = 50;

	@Configuration({"temp_top", "temp_mid", "temp_bot"})
	private ChannelCollection temperatures;

	private final List<Temperature> temperatureHighFlags = new ArrayList<Temperature>(Temperature.values().length);

	@Configuration
	private ChannelListener state;

	private Value stateValueLast = null;
	private volatile long startTimeLast = 0;

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		
		temperatures.register(Temperature.TOP, new TemperatureListener(this, Temperature.TOP));
		temperatures.register(Temperature.CENTER, new TemperatureListener(this, Temperature.CENTER));
		temperatures.register(Temperature.BOTTOM, new TemperatureListener(this, Temperature.BOTTOM));
		state.registerValueListener(new StateListener());
	}

	@Override
	public void onDeactivate() {
		temperatures.deregister();
		state.deregister();
	}

	public void start() {
		state.write(new BooleanValue(true));
	}

	public void stop() {
		state.write(new BooleanValue(false));
	}

	@Override
	public synchronized void onTemperatureReceived(Temperature type, Double temperature) {
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

	private class StateListener implements ValueListener {

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
	}

}
