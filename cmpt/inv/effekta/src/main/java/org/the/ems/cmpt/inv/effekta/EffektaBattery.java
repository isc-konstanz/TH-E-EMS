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
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class EffektaBattery extends ElectricalEnergyStorage {
	private final static Logger logger = LoggerFactory.getLogger(Effekta.class);

	@Configuration
	private double chargeVoltageMax = 52;

	@Configuration
	private double dischargeVoltageMin = 48;

	@Configuration
	private Channel currentMax;

	@Configuration
	private Channel voltageSetpoint;

	@Configuration
	private Channel power;

	@Configuration
	private Channel soc;

	@Configuration
	private Channel current;

	@Configuration
	protected ChannelListener voltage;

	@Override
	public Value getVoltage() throws ComponentException, InvalidValueException {
		return voltage.getLatestValue();
	}

	@Override
	public Value getPower() throws InvalidValueException {
		return new DoubleValue(current.getLatestValue().doubleValue() * voltage.getLatestValue().doubleValue());
	}

	@Override
	public Value getStateOfCharge() throws InvalidValueException {
		return soc.getLatestValue();
	}

	public void initialize() {
		power.setLatestValue(new DoubleValue(0));
		soc.setLatestValue(new DoubleValue(50));
		current.setLatestValue(new DoubleValue(0));
	}

	public void setStateOfCharge(Value value) {
		soc.setLatestValue(value);
	}

	public Value socEstimation(Long time) throws InvalidValueException {
		long socTime = getStateOfCharge().getTime();
		double energy;
		double socEstimation;
		double current = getCurrent().doubleValue();

		if (voltage.getLatestValue().doubleValue() >= getVoltageMax()) {
			setStateOfCharge(new DoubleValue(100));
		} else if (voltage.getLatestValue().doubleValue() <= getVoltageMin()) {
			setStateOfCharge(new DoubleValue(0));
		} else if (!Double.isNaN(current)) {
			energy = current * voltage.getLatestValue().doubleValue() * (time - socTime) / (1000 * 3600);
			socEstimation = getStateOfCharge().doubleValue() - energy / (getCapacity() * 1000) * 100;
			socEstimation = Math.max(0, socEstimation);
			socEstimation = Math.min(100, socEstimation);

			setStateOfCharge(new DoubleValue(socEstimation, time));
		}
		
		return getStateOfCharge();
	}

	void registerVoltageListener(ValueListener listener) {
		voltage.registerValueListener(listener);
	}

	void deregister() {
		voltage.deregister();
	}

	public Channel getCurrentMax() {
		return currentMax;
	}

	public Value getCurrent() throws InvalidValueException {
		return current.getLatestValue();
	}

	public double getVoltageMax() {
		return chargeVoltageMax;
	}

	public double getVoltageMin() {
		return dischargeVoltageMin;
	}

	public Channel getVoltageSetpoint() {
		return voltageSetpoint;
	}

	public void setCurrent(Value value) {
		current.setLatestValue(value);
	}

	public void setPower(Value value) {
		power.setLatestValue(value);

		try {
			current.setLatestValue(new DoubleValue(value.doubleValue() / voltage.getLatestValue().doubleValue()));
		} catch (InvalidValueException e) {
			logger.warn("Obligatory value missing: {}", e.getMessage());
		}
	}

	public double getChargeVoltageMax() {
		return chargeVoltageMax;
	}
}
