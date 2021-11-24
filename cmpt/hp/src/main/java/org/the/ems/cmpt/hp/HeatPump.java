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
package org.the.ems.cmpt.hp;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.Heating;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = HeatPumpService.class,
	configurationPid = HeatPumpService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class HeatPump extends Heating implements HeatPumpService {
	private static final Logger logger = LoggerFactory.getLogger(HeatPump.class);

	@Configuration("temp_in_max")
	protected double temperatureInMax;

	@Configuration("temp_in")
	protected Channel temperature;

	protected ValueListener temperatureListener;
	protected Value temperatureValue = DoubleValue.emptyValue();

	@Configuration
	protected double cop;

	@Override
	public Value getCoefficientOfPerformance() throws ComponentException {
		return new DoubleValue(cop);
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		temperatureListener = new TemperatureListener();
		temperature.registerValueListener(temperatureListener);
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		temperature.deregisterValueListener(temperatureListener);
	}

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		if (temperatureValue.doubleValue() > temperatureInMax) {
			throw new ComponentException("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
		}
	}

	@Override
	protected void onStateChanged(Value value) {
		if (value.booleanValue() && temperatureValue.doubleValue() > temperatureInMax) {
			logger.warn("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
			// TODO: implement virtual start signal that does not affect relay
			try {
				stop();
				
			} catch (EnergyManagementException e) {
				logger.warn("Error while switching off heat pump due to temperature threshold violation: {}",
						e.getMessage());
			}
		}
	}

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			temperatureValue = value;
			if (temperatureValue.doubleValue() >= temperatureInMax) {
				try {
					stop();
					
				} catch (EnergyManagementException e) {
					logger.warn("Error while switching off heat pump due to temperature threshold violation: {}",
							e.getMessage());
				}
			}
		}
	}

}
