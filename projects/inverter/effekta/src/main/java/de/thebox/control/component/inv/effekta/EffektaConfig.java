package de.thebox.control.component.inv.effekta;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.inv.InverterConfig;
import de.thebox.control.core.config.ConfigurationException;

public class EffektaConfig extends InverterConfig {

	public final static String OBJECTIVE = "objective";

	public EffektaConfig(Preferences config) {
		super(config);
	}

	public String getObjective() throws ConfigurationException {
		return getKey(OBJECTIVE);
	}

}
