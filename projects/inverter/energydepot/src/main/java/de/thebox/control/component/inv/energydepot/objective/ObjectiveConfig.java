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

	public ObjectiveConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getObjective() throws ComponentConfigException {
		return getKey(OBJECTIVE_KEY);
	}

}
