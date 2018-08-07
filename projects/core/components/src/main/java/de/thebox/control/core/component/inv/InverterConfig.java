package de.thebox.control.core.component.inv;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

public class InverterConfig extends Configuration {

	public final static String SECTION = "Inverter";

	public final static String OBJECTIVE_MAX_KEY = "powerMax";
	public final static String OBJECTIVE_MIN_KEY = "powerMin";
	public final static Value OBJECTIVE_DEFAULT = DoubleValue.emptyValue();

	public final static String CONSUMPTION_POWER_KEY = "consPower";

	public final static String BATTERY_SOC_KEY = "batState";
	public final static String BATTERY_SOC_MIN_KEY = "batStateMin";
	public final static double BATTERY_SOC_MIN_DEFAULT = 10;

	public InverterConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public int getPowerMax() throws ConfigurationException {
		return (int) (getDouble(OBJECTIVE_MAX_KEY)*1000);
	}

	public int getPowerMin() throws ConfigurationException {
		return (int) (getDouble(OBJECTIVE_MIN_KEY)*1000);
	}

	public String getConsumptionPower() throws ConfigurationException {
		return getKey(CONSUMPTION_POWER_KEY);
	}

	public String getBatterySoC() throws ConfigurationException {
		return getKey(BATTERY_SOC_KEY);
	}

	public double getBatteryStateMin() {
		return config.getDouble(BATTERY_SOC_MIN_KEY, BATTERY_SOC_MIN_DEFAULT);
	}

}
