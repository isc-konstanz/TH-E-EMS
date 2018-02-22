package org.openmuc.framework.app.thebox.control;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class ControlConfig extends Configuration {

	public final static String SECTION = "Control";

	public final static String ENABLE_KEY = "enable";

	public ControlConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getEnabled() throws ConfigurationException {
		return getKey(ENABLE_KEY);
	}

}
