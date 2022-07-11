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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.ComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class EffektaBattery extends ElectricalEnergyStorage {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);

	private final static String SECTION = "Storage";

	@Configuration("voltage_charge_max")
	private double chargeVoltageMax = 52;

	@Configuration("voltage_discharge_min")
	private double dischargeVoltageMin = 48;

	@Configuration
	private Channel voltageSetpoint;

	@Configuration
	private Channel voltage;

	@Configuration
	private Channel current;

	@Configuration
	private Channel currentMax;

	@Configuration("current_charge")
	private Channel chargeCurrent;

	@Configuration("current_discharge")
	private Channel dischargeCurrent;

	@Configuration
	private Channel power;

	@Configuration
	private Channel soc;

	protected EffektaBattery() {
		super(SECTION);
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);

		power.setLatestValue(new DoubleValue(0));
		soc.setLatestValue(new DoubleValue(50));
	}

	@Override
	protected void onDeactivate() {
		voltage.deregisterValueListeners();
	}

	@Override
	public Value getVoltage() throws ComponentException, InvalidValueException {
		return voltage.getLatestValue();
	}

	@Override
	public void registerVoltageListener(ValueListener listener) {
		voltage.registerValueListener(listener);
	}

	@Override
	public void deregisterVoltageListener(ValueListener listener) throws ComponentException {
		voltage.deregisterValueListener(listener);
	}

	public Channel getVoltageSetpoint() {
		return voltageSetpoint;
	}

	public double getVoltageMin() {
		return dischargeVoltageMin;
	}

	public double getVoltageMax() {
		return chargeVoltageMax;
	}

	public Channel getCurrentMax() {
		return currentMax;
	}

	public void setCurrent(Value value) {
		current.setLatestValue(value);
	}

	public Value getCurrent() throws InvalidValueException {
		return current.getLatestValue();
	}

	public Value getChargeCurrent() throws InvalidValueException {
		return chargeCurrent.getLatestValue();
	}

	public Value getDischargeCurrent() throws InvalidValueException {
		return dischargeCurrent.getLatestValue();
	}

	public void setPower(Value value) {
		power.setLatestValue(value);
		try {
			current.setLatestValue(new DoubleValue(value.doubleValue() / voltage.getLatestValue().doubleValue()));
			
		} catch (InvalidValueException e) {
			logger.warn("Obligatory value missing: {}", e.getMessage());
		}
	}

	@Override
	public Value getPower() throws InvalidValueException {
		return new DoubleValue(current.getLatestValue().doubleValue() * voltage.getLatestValue().doubleValue());
	}

	@Override
	public Value getStateOfCharge() throws InvalidValueException {
		return soc.getLatestValue();
	}

	public Value processStateOfCharge(long time) throws InvalidValueException {
		long timeLast = getStateOfCharge().getEpochMillis();
		double current = getCurrent().doubleValue();
		double value;
		if (voltage.getLatestValue().doubleValue() >= getVoltageMax()) {
			setStateOfCharge(new DoubleValue(100));
		}
		else if (voltage.getLatestValue().doubleValue() <= getVoltageMin()) {
			setStateOfCharge(new DoubleValue(0));
		}
		else if (!Double.isNaN(current)) {
			double energy = current * voltage.getLatestValue().doubleValue() * (time - timeLast) / (1000 * 3600);
			value = getStateOfCharge().doubleValue() - energy / (getCapacity() * 1000) * 100;
			if (value < 0) {
				value = 0;
			}
			if (value > 100) {
				value = 100;
			}
			setStateOfCharge(new DoubleValue(value, time));
		}
		return getStateOfCharge();
	}

	public void setStateOfCharge(Value value) {
		soc.setLatestValue(value);
	}

}
