package org.the.ems.cmpt.inv.effekta;

import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class EffektaBattery extends ElectricalEnergyStorage {
	@Configuration
	private double chargeVoltageMax = 53;

	@Configuration
	private double dischargeVoltageMin = 48;
	
	@Configuration
	private Channel currentMax;

	@Configuration
	private Channel dischargeCurrent;

	@Configuration
	private Channel voltageSetpoint;

	@Configuration
	private Channel chargeCurrent;

	@Configuration
	private Channel power;

	@Configuration
	protected ChannelListener current;

	@Configuration
	protected ChannelListener voltage;

	@Configuration
	protected ChannelListener soc;
	
	
	@Override
	public Value getVoltage() {
		return voltage.getLatestValue();
	}

	@Override
	public Value getStateOfCharge() {
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
	
	public Value getCurrent() {
		return current.getLatestValue();
	}
	
	public double getPower() {
		return current.getLatestValue().doubleValue() * voltage.getLatestValue().doubleValue();
	}

	public double getMaxVoltage() {
		return chargeVoltageMax;
	}

	public double getMinVoltage() {
		return dischargeVoltageMin;
	}

	public Channel getChargeCurrent() {
		return chargeCurrent;
	}

	public Channel getDischargeCurrent() {
		return dischargeCurrent;
	}

	public Channel getVoltageSetpoint() {
		return voltageSetpoint;
	}

	public void setPower(Value value) {
		power.setLatestValue(value);
	}
}
