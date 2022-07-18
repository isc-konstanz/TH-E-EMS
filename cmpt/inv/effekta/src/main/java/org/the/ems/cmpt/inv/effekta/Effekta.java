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
package org.the.ems.cmpt.inv.effekta;

import java.util.Arrays;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.inv.Inverter;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE, 
	service = InverterService.class,
	configurationPid = InverterService.PID+".effekta", 
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Effekta extends Inverter<EffektaBattery> implements ValueListener {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);

	@Configuration
	protected Channel power;

	@Configuration
	protected Channel powerImport;

	@Configuration
	protected Channel powerExport;

	private volatile long timestampLast = Long.MIN_VALUE;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		setpoint.setLatestValue(DoubleValue.zeroValue());

		//powerImport.registerValueListener(this);
		powerExport.registerValueListener(this);
	}

	@Override
	public void onDeactivate() throws ComponentException {
		powerExport.deregisterValueListeners();
	}

	@Override
	public void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		//long timestamp = value.getEpochMillis();
		// TODO: implement setpoint including DC inputs
		try {
			storage.set(value);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to set battery charging current: {}", e.getMessage());
		}
	}

	@Override
	public void onValueReceived(Value value) {
		long timeMax = -1;
		try {
			for (Channel channel : Arrays.asList(powerImport, powerExport)) {
				Value powerValue = channel.getLatestValue();
				if (powerValue.getEpochMillis() <= timestampLast) {
					return;
				}
				if (powerValue.getEpochMillis() > timeMax) {
					timeMax = powerValue.getEpochMillis();
				}
			}
			timestampLast = timeMax;
			power.setLatestValue(new DoubleValue(
					powerImport.getLatestValue().doubleValue() - 
					powerExport.getLatestValue().doubleValue(), timestampLast));
			
		} catch (InvalidValueException e) {
			logger.debug("Unable to calculate weighted storage temperature: {}", e.getMessage());
		}
	}

}
