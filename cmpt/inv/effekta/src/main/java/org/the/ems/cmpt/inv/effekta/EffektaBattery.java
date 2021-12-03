package org.the.ems.cmpt.inv.effekta;

import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;


public class EffektaBattery extends ElectricalEnergyStorage {

	private final static String SECTION = "Storage";

	@Configuration
	private double chargeVoltageMax = 53;

	@Configuration
	private double chargeVoltageMin = 48;

	@Configuration
	protected Channel chargeCurrent;

	@Configuration
	protected Channel dischargeCurrent;

	protected EffektaBattery() {
		super(SECTION);
	}

	public double getChargeVoltage() {
		return chargeVoltageMax;
	}

	public double getDischargeVoltage() {
		return chargeVoltageMin;
	}

	public Value getChargeCurrent() throws InvalidValueException {
		return chargeCurrent.getLatestValue();
	}

	public Value getDischargeCurrent() throws InvalidValueException {
		return dischargeCurrent.getLatestValue();
	}

}
