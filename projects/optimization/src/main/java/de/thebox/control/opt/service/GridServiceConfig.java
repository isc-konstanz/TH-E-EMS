package de.thebox.control.opt.service;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class GridServiceConfig extends Configuration {

	public final static String SECTION = "External";

	public final static String GRID_SERVICE_KEY = "gridService";
	public final static String FEED_EXTERNAL_KEY = "externalServiceFeed";

	public GridServiceConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getGridService() throws ConfigurationException {
		return getKey(GRID_SERVICE_KEY);
	}

	public String getExternalServiceFeed() throws ConfigurationException {
		return config.get(FEED_EXTERNAL_KEY, null);
	}

}
