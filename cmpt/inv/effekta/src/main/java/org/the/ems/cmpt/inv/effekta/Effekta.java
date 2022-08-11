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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.the.ems.core.data.ChannelCollection;
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

	@Configuration(value = "power_output_ac", mandatory = false)
	protected Channel alternatingPowerOutput;

	@Configuration(value = "power_input_ac")
	protected Channel alternatingPowerInput;

	@Configuration(value = "power_input_dc")
	protected Channel directPowerInput;

	@Configuration(value = "power_input_dc_*")
	private ChannelCollection directPowerInputs;

	private volatile Value setpointValue = DoubleValue.zeroValue();

	private volatile long timestampLast = Long.MIN_VALUE;

	private List<Channel> getPowers() {
		return Stream.of(Arrays.asList(alternatingPowerOutput, alternatingPowerInput), directPowerInputs.values())
				.flatMap(c -> c.stream()).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	public Value getInputPower() throws ComponentException, InvalidValueException {
		return directPowerInput.getLatestValue();
	}

	@Override
	public Value getInputPower(ValueListener listener) throws ComponentException, InvalidValueException {
		return directPowerInput.getLatestValue(listener);
	}

	@Override
	public void registerInputPowerListener(ValueListener listener) throws ComponentException {
		directPowerInput.registerValueListener(listener);
	}

	@Override
	public void deregisterInputPowerListener(ValueListener listener) throws ComponentException {
		directPowerInput.deregisterValueListener(listener);
	}

	@Override
	public Value getActivePower() throws ComponentException, InvalidValueException {
		return power.getLatestValue();
	}

	@Override
	public Value getActivePower(ValueListener listener) throws ComponentException, InvalidValueException {
		return power.getLatestValue(listener);
	}

	@Override
	public void registerActivePowerListener(ValueListener listener) throws ComponentException {
		power.registerValueListener(listener);
	}

	@Override
	public void deregisterActivePowerListener(ValueListener listener) throws ComponentException {
		power.deregisterValueListener(listener);
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		setpoint.setLatestValue(DoubleValue.zeroValue());
		
		for (Channel power: getPowers()) {
			power.registerValueListener(this);
		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		if (alternatingPowerOutput != null) {
			alternatingPowerOutput.deregisterValueListener(this);
		}
		alternatingPowerInput.deregisterValueListener(this);
		directPowerInputs.deregisterValueListeners();
	}

	@Override
	public void onSetpointUpdate(WriteContainer container, Value setpoint) throws ComponentException {
		long timestamp =  setpoint.getEpochMillis();
		try {
//			double inputPower = getInputPower().doubleValue();
//			double setpointError = setpoint.doubleValue() - getActivePower().doubleValue();
//			double setpointPower = setpointValue.doubleValue()+setpointError;
//			
//			if (setpointPower > 0 && !storage.isDischargable()) {
//				
//			}
//			if (setpointPower < 0 && !storage.isChargable()) {
//				
//			}
//			setpointValue = new DoubleValue(setpointPower, timestamp);
//			storage.set(setpointValue);
			storage.set(setpoint);
			
		} catch (EnergyManagementException e) {
			logger.warn("Unable to set battery charging setpoint: {}", e.getMessage());
		}
	}

	@Override
	public void onValueReceived(Value value) {
		long timeMax = -1;
		try {
			double directPowerValue = 0;
			
			for (Channel power : getPowers()) {
				Value powerValue = power.getLatestValue();
				if (powerValue.getEpochMillis() <= timestampLast) {
					return;
				}
				if (powerValue.getEpochMillis() > timeMax) {
					timeMax = powerValue.getEpochMillis();
				}
				if (directPowerInputs.containsValue(power)) {
					directPowerValue += powerValue.doubleValue();
				}
			}
			directPowerInput.setLatestValue(new DoubleValue(directPowerValue, timeMax));
			
			double alternatingPowerValue = -directPowerValue;
			if (alternatingPowerOutput != null) {
				alternatingPowerValue += alternatingPowerOutput.getLatestValue().doubleValue();
			}
			alternatingPowerValue += alternatingPowerInput.getLatestValue().doubleValue();
			alternatingPowerValue += storage.getPower().doubleValue();
			
			power.setLatestValue(new DoubleValue(alternatingPowerValue, timeMax));
			
			timestampLast = timeMax;
			onSetpointUpdate();
			
		} catch (InvalidValueException e) {
			logger.debug("Unable to calculate generalized power values: {}", e.getMessage());
		}
	}

}
