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
public class EffektaInverter extends Inverter<EffektaBattery> implements ValueListener {
	private final static Logger logger = LoggerFactory.getLogger(EffektaInverter.class);

	@Configuration
	protected Channel activePower;

	@Configuration(value = "output_power_ac", mandatory = false)
	protected Channel alternatingPowerOutput;

	@Configuration(value = "input_power_ac")
	protected Channel alternatingPowerInput;

	@Configuration(value = "input_power_dc")
	protected Channel directPowerInput;

	@Configuration(value = "input_power_dc_*")
	private ChannelCollection directPowerInputs;

	@Configuration(mandatory = false, scale = 1000)
	private int storageSetpointChangeDelay = 10000;

	@Configuration(mandatory = false)
	private double storageSetpointChangeMax = 1000;

	private volatile double storageSetpointError = 0;
	private volatile Value storageSetpoint = DoubleValue.zeroValue();

	private volatile long storageSetpointUpdateTime = Long.MIN_VALUE;
	private volatile long setpointUpdateTime = Long.MIN_VALUE;

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
		return activePower.getLatestValue();
	}

	@Override
	public Value getActivePower(ValueListener listener) throws ComponentException, InvalidValueException {
		return activePower.getLatestValue(listener);
	}

	@Override
	public void registerActivePowerListener(ValueListener listener) throws ComponentException {
		activePower.registerValueListener(listener);
	}

	@Override
	public void deregisterActivePowerListener(ValueListener listener) throws ComponentException {
		activePower.deregisterValueListener(listener);
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
		long timestamp =  System.currentTimeMillis();
		try {
			double setpointPower = setpoint.doubleValue();
			if (setpointPower == 0) {
				storageSetpoint = new DoubleValue(setpointPower, timestamp);
				storageSetpointError = 0;
				storageSetpointUpdateTime = timestamp;
			}
			else if (storage.isReady()) {
				// Wait 10 seconds per kW power setpoint
				int storageSetpointChangePause = (int) Math.round(Math.abs(
						Math.max(storageSetpointChangeDelay * storageSetpointError/1000, storageSetpointChangeDelay)));
				if (storageSetpointChangePause <= timestamp - storageSetpointUpdateTime || storageSetpointUpdateTime < 0) {
					storageSetpointError = setpointPower - getActivePower().doubleValue();
					if (storageSetpoint.doubleValue() == 0 && storage.getPower().doubleValue() > storage.getPowerMin()) {
						storageSetpointError += getInputPower().doubleValue();
					}
					
					// Dampen change possible at once
					if (Math.abs(storageSetpointError) > storageSetpointChangeMax) {
						logger.debug("Dampen storage setpoint error: {}", storageSetpointError);
						if (storageSetpointError > 0) {
							storageSetpointError = storageSetpointChangeMax;
						}
						else if (storageSetpointError < 0) {
							storageSetpointError = -storageSetpointChangeMax;
						}
					}
					
					setpointPower = storageSetpoint.doubleValue() + storageSetpointError;
					if (setpointPower > getMaxPower()) {
						setpointPower = getMaxPower();
					}
					else if (setpointPower < getMinPower()) {
						setpointPower = getMinPower();
					}
					if (Math.abs(setpointPower) < storage.getPowerMin()) {
						if (setpointPower > 0) {
							setpointPower = storage.getPowerMin();
						}
						else if (setpointPower < 0) {
							setpointPower = -storage.getPowerMin();
						}
					}
					storageSetpoint = new DoubleValue(setpointPower, timestamp);
					storageSetpointUpdateTime = timestamp;
				}
			}
			else {
				// Set latest changed time while the storage is not ready, to start waiting for the setpoint value
				// just after the battery mode was applied
				storageSetpointUpdateTime = timestamp;
			}
			storage.set(storageSetpoint);
			
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
				if (powerValue.getEpochMillis() <= setpointUpdateTime) {
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
			
			activePower.setLatestValue(new DoubleValue(alternatingPowerValue, timeMax));
			
			setpointUpdateTime = timeMax;
			onSetpointUpdate();
			
		} catch (InvalidValueException e) {
			logger.debug("Unable to calculate generalized power values: {}", e.getMessage());
		}
	}

}
