package de.thebox.control.component.inv.energydepot.consumption;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfig;
import de.thebox.control.core.component.ComponentConfigException;

public class ConsumptionConfig extends ComponentConfig {

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

	public String getConsumptionPower() throws ComponentConfigException {
		return getKey(CONSUMPTION_POWER_KEY);
	}

	public String getBatteryPower() throws ComponentConfigException {
		return getKey(BATTERY_POWER_KEY);
	}

	public String getAcPower() throws ComponentConfigException {
		return getKey(AC_POWER_KEY);
	}

	public String getDcPower() throws ComponentConfigException {
		return getKey(DC_POWER_KEY);
	}

}
