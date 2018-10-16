package de.thebox.control.component.inv.effekta;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class BatteryConfig extends Configuration {

	public final static String SECTION = "Battery";

	public final static String MODE = "batMode";
	public final static String CHARGE_CURRENT = "batChargeCurrent";
	public final static String DISCHARGE_CURRENT = "batDischargeCurrent";
	public final static String VOLTAGE = "batVoltage";

	public BatteryConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getMode() throws ConfigurationException {
		return getKey(MODE);
	}

	public String getChargeCurrent() throws ConfigurationException {
		return getKey(CHARGE_CURRENT);
	}

	public String getDischargeCurrent() throws ConfigurationException {
		return getKey(DISCHARGE_CURRENT);
	}
	
	public String getVoltage() throws ConfigurationException {
		return getKey(VOLTAGE);
	}
}
