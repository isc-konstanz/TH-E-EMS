package de.thebox.control.core.component.inv.external;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class ExternalConfig extends Configuration {

	public final static String SECTION = "External";

	public final static String ENABLE_KEY = "enable";

	public final static String POWER_VIRTUAL_KEY = "virtualPower";
	public final static String POWER_ACTIVE_KEY = "activePower";

	public final static String REMOTE_PV_KEY = "pvPower";

	public ExternalConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getEnabled() throws ConfigurationException {
		return getKey(ENABLE_KEY);
	}

	public String getVirtualPower() throws ConfigurationException {
		return getKey(POWER_VIRTUAL_KEY);
	}

	public String getActualPower() throws ConfigurationException {
		return getKey(POWER_ACTIVE_KEY);
	}

	public String getPvPower() throws ConfigurationException {
		return config.get(REMOTE_PV_KEY, null);
	}

}
