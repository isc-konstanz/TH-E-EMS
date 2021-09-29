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

	@Configuration
	private double chargeVoltageMax = 52;

	@Configuration
	private double dischargeVoltageMin = 48;
	
	@Configuration
	private Channel currentMax;

	@Configuration
	private Channel currentExport;

	@Configuration
	private Channel voltageSetpoint;

	@Configuration
	private Channel currentImport;

	@Configuration
	private Channel power;

	@Configuration
	protected ChannelListener current;

	@Configuration
	protected ChannelListener voltage;

	@Configuration
	protected ChannelListener soc;

	@Override
	public Value getVoltage() throws ComponentException, InvalidValueException {
		return voltage.getLatestValue();
	}

	@Override
	public Value getStateOfCharge() throws ComponentException, InvalidValueException {
		return soc.getLatestValue();
	}

	void registerStateOfChargeListener(ValueListener listener) {
		soc.registerValueListener(listener);
	}

	void deregisterStateOfChargeListener() {
		soc.deregister();
	}

	void registerVoltageListener(ValueListener listener) {
		voltage.registerValueListener(listener);
	}

	void registerCurrentListener(ValueListener listener) {
		current.registerValueListener(listener);
	}

	void deregister() {
		soc.deregister();
        voltage.deregister();
        current.deregister();
	}
	
	public Channel getMaxCurrent() {
		return currentMax;
	}
	
	public Value getCurrent() throws InvalidValueException {
		return current.getLatestValue();
	}
	
	@Override
	public Value getPower() throws InvalidValueException {
		return new DoubleValue(current.getLatestValue().doubleValue() * voltage.getLatestValue().doubleValue());
	}

	public double getMaxVoltage() {
		return chargeVoltageMax;
	}

	public double getMinVoltage() {
		return dischargeVoltageMin;
	}

	public Channel getCurrentImport() {
		return currentImport;
	}

	public Channel getCurrentExport() {
		return currentExport;
	}

	public Channel getVoltageSetpoint() {
		return voltageSetpoint;
	}

	public void setPower(Value value) {
		power.setLatestValue(value);
	}
}
