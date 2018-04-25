package org.openmuc.framework.app.thebox.control;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class ControlConfig extends Configuration {

	public final static String SECTION = "Control";

	public final static String MAINTENANCE_KEY = "maintenance";

	public ControlConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getMaintenance() throws ConfigurationException {
		return getKey(MAINTENANCE_KEY);
	}

}
