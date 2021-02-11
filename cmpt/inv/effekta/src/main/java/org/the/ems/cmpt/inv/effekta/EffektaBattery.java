package org.the.ems.cmpt.inv.effekta;

import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;


public class EffektaBattery extends ElectricalEnergyStorage {
	
//	@Configuration(mandatory=false)
//	public final static short CURRENT_MIN = 10;
	
//	@Configuration(mandatory=false)
//	public final static short CURRENT_MAX = 100;
	
	@Configuration
	private double chargeVoltageMax = 53;
	
	@Configuration
	private double chargeVoltageMin = 48;
	
	@Configuration
	private Channel voltage;
	
	@Configuration
	protected ChannelListener soc;
	
	@Configuration
	private Channel power;

	@Override
	public Value getVoltage() {
		return voltage.getLatestValue();
	}
	
	@Override
	public Value getStateOfCharge() {
		return soc.getLatestValue();
	}
	
	@Override
	public Value getChargePower() {
		return power.getLatestValue();
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

}
