package de.thebox.control.component.inv.energydepot.external;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfig;
import de.thebox.control.core.component.ComponentConfigException;

public class ExternalConfig extends ComponentConfig {

	public final static String SECTION = "External";

	public final static String ENABLE_KEY = "enable";

	public final static String POWER_VIRTUAL_KEY = "virtualPower";
	public final static String POWER_ACTUAL_KEY = "actualPower";

	public final static String FEED_PV_KEY = "pvFeed";

	public ExternalConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getEnabled() throws ComponentConfigException {
		return getKey(ENABLE_KEY);
	}

	public String getVirtualPower() throws ComponentConfigException {
		return getKey(POWER_VIRTUAL_KEY);
	}

	public String getActualPower() throws ComponentConfigException {
		return getKey(POWER_ACTUAL_KEY);
	}

	public String getPvFeed() throws ComponentConfigException {
		return config.get(FEED_PV_KEY, null);
	}

}
