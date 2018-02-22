package de.thebox.control.component.inv.energydepot.consumption;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class ConsumptionConfig extends Configuration {

	public final static String SECTION = "Consumption";

	public final static String CONSUMPTION_POWER_KEY = "consPower";
	public final static String BATTERY_POWER_KEY = "batPower";
	public final static String AC_POWER_KEY = "acPower";
	public final static String DC_POWER_KEY = "dcPower";

	public ConsumptionConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getConsumptionPower() throws ConfigurationException {
		return getKey(CONSUMPTION_POWER_KEY);
	}

	public String getBatteryPower() throws ConfigurationException {
		return getKey(BATTERY_POWER_KEY);
	}

	public String getAcPower() throws ConfigurationException {
		return getKey(AC_POWER_KEY);
	}

	public String getDcPower() throws ConfigurationException {
		return getKey(DC_POWER_KEY);
	}

}
