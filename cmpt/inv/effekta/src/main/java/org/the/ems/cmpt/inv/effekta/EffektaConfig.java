package de.thebox.control.component.inv.effekta;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.inv.InverterConfig;
import de.thebox.control.core.config.ConfigurationException;

public class EffektaConfig extends InverterConfig {

	public final static String CONSUMPTION_POWER = "consPower";
	public final static String DC_POWER_1 = "dcPower1";
	public final static String DC_POWER_2 = "dcPower2";

	public EffektaConfig(Preferences config) {
		super(config);
	}
	
	public String getConsumptionPower() throws ConfigurationException {
		return getKey(CONSUMPTION_POWER);
	}
	
	public String getDCPower1() throws ConfigurationException {
		return getKey(DC_POWER_1);
	}
	
	public String getDCPower2() throws ConfigurationException {
		return getKey(DC_POWER_2);
	}
	
}
