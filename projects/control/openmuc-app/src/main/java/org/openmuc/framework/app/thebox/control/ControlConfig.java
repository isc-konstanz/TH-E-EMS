package org.openmuc.framework.app.thebox.control;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfig;
import de.thebox.control.core.component.ComponentConfigException;

public class ControlConfig extends ComponentConfig {

	public final static String SECTION = "Control";

	public final static String ENABLE_KEY = "enable";

	public ControlConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getEnabled() throws ComponentConfigException {
		return getKey(ENABLE_KEY);
	}

}
