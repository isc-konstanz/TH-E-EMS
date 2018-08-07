package de.thebox.control.component.inv.energydepot;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.inv.InverterConfig;
import de.thebox.control.core.config.ConfigurationException;

public class EnergyDepotConfig extends InverterConfig {

	public final static String OBJECTIVE = "objective";
	public final static String OBJECTIVE_CONTROL_KEY = "control";

	public EnergyDepotConfig(Preferences config) {
		super(config);
	}

	public String getObjective() throws ConfigurationException {
		return getKey(OBJECTIVE);
	}

	public String getObjectiveControl() throws ConfigurationException {
		return getKey(OBJECTIVE_CONTROL_KEY);
	}

}
