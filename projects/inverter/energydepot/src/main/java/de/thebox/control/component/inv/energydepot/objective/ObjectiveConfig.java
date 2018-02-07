package de.thebox.control.component.inv.energydepot.objective;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfig;
import de.thebox.control.core.component.ComponentConfigException;

public class ObjectiveConfig extends ComponentConfig {

	public final static String SECTION = "Inverter";

	public final static String OBJECTIVE_KEY = "invObjective";

	public final static double OBJECTIVE_MAX = 10000;
	public final static double OBJECTIVE_MIN = -10000;
	public final static double OBJECTIVE_DEFAULT = 0;

	public final static String BATTERY_SOC_KEY = "batState";
	public final static String BATTERY_SOC_MIN_KEY = "batStateMin";
	public final static double BATTERY_SOC_MIN__DEFAULT = 10;

	public ObjectiveConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getObjective() throws ComponentConfigException {
		return getKey(OBJECTIVE_KEY);
	}

	public String getBatterySoC() throws ComponentConfigException {
		return getKey(BATTERY_SOC_KEY);
	}

	public double getBatteryStateMin() {
		return config.getDouble(BATTERY_SOC_MIN_KEY, BATTERY_SOC_MIN__DEFAULT);
	}

}
