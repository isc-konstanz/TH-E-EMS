package de.thebox.control.core.component.inv.consumption;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class ConsumptionConfig extends Configuration {

	public final static String SECTION = "Consumption";

	public final static String BATTERY_POWER_KEY = "batPower";
	public final static String AC_POWER_KEY = "acPower";
	public final static String DC_POWER_KEY = "dcPower";
	public final static String DC1_POWER_KEY = "dc1Power";
	public final static String DC2_POWER_KEY = "dc2Power";

	public ConsumptionConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
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

	public String getDcPower1() throws ConfigurationException {
		return getKey(DC1_POWER_KEY);
	}

	public String getDcPower2() throws ConfigurationException {
		return getKey(DC2_POWER_KEY);
	}

	public boolean hasDcPower2() {
		String value = config.get(DC2_POWER_KEY, null);
		if (value != null && !value.isEmpty()) {
			return true;
		}
		return false;
	}

}
