package de.thebox.control.component.inv.energydepot;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class EnergyDepotConfig extends Configuration {

	public final static String SECTION = "Inverter";

	public final static String OBJECTIVE_LOCAL_KEY = "invObjective";

	public EnergyDepotConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getInverterObjective() throws ConfigurationException {
		return getKey(OBJECTIVE_LOCAL_KEY);
	}

}
