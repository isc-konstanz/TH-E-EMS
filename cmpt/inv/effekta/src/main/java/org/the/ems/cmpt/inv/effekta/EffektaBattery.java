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
	private double chargeVoltageMin = 48;
	
	@Configuration
	private Channel voltage;

	@Configuration
	private Channel dischargeCurrent;

	@Configuration
	private Channel chargeCurrent;
	
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
	
	void deregister() {
		soc.deregister();
	}
	
	public double getChargeVoltage() {
		return chargeVoltageMax;
	}
	
	public double getDischargeVoltage() {
		return chargeVoltageMin;
	}
	
	public Channel getChargeCurrent() {
		return chargeCurrent;
	}
	
	public Channel getDischargeCurrent() {
		return dischargeCurrent;
	}
}
