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
import org.the.ems.core.settings.HeatingSettings;
import org.the.ems.core.settings.ValueSettings;

@Component(
	scope = ServiceScope.BUNDLE,
	service = HeatPumpService.class,
	configurationPid = HeatPumpService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class HeatPump extends Heating implements HeatPumpService {
	private static final Logger logger = LoggerFactory.getLogger(HeatPump.class);

	@Configuration("flow_temp_max")
	protected double temperatureFlowMax;

	@Configuration("flow_temp")
	protected Channel temperatureFlow;
	protected Value temperatureFlowValue = DoubleValue.zeroValue();

	@Configuration
	protected double cop;

	@Override
	public Value getCoefficientOfPerformance() throws ComponentException {
		return new DoubleValue(cop);
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		temperatureFlow.registerValueListener(new TemperatureListener());
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		super.onStart(container, settings);
		
		Value value = settings.getValue();
		if (value.doubleValue() < getMaxPower()) {
			onStart(container, HeatingSettings.newHeatingWaterHeating(settings.getEpochMillis()));
		}
		else {
			onStart(container, HeatingSettings.newDomesticWaterHeating(settings.getEpochMillis()));
		}
	}

	@Override
	protected void onStateChanged(Value value) {
		if (value.booleanValue() && temperatureFlowValue.doubleValue() > temperatureFlowMax) {
			logger.warn("Unable to switch on heat pump: Heating flow input temperature above threshold: {}", 
					temperatureFlowValue.doubleValue());

			// TODO: implement virtual start signal that does not affect relay
			try {
				ValueSettings stopSettings = ValueSettings.ofBoolean(false, value.getEpochMillis());
				stopSettings.setEnforced(true);
				stop(stopSettings);
				
			} catch (EnergyManagementException e) {
				logger.warn("Error while switching off heat pump due to temperature threshold violation: {}",
						e.getMessage());
			}
		}
	}

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			temperatureFlowValue = value;
			if (temperatureFlowValue.doubleValue() >= temperatureFlowMax) {
				try {
					ValueSettings stopSettings = ValueSettings.ofBoolean(false, value.getEpochMillis());
					stopSettings.setEnforced(true);
					stop(stopSettings);
					
				} catch (EnergyManagementException e) {
					logger.warn("Error while switching off heat pump due to temperature threshold violation: {}",
							e.getMessage());
				}
			}
		}
	}

}
