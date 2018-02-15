package de.thebox.control.opt.service;

import java.util.prefs.Preferences;

import de.thebox.control.core.component.ComponentConfig;
import de.thebox.control.core.component.ComponentConfigException;

public class GridServiceConfig extends ComponentConfig {

	public final static String SECTION = "External";

	public final static String GRID_SERVICE_KEY = "gridService";

	public final static String FEED_EXPORT_KEY = "externalExportFeed";
	public final static String FEED_IMPORT_KEY = "externalImportFeed";

	public GridServiceConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getGridService() throws ComponentConfigException {
		return getKey(GRID_SERVICE_KEY);
	}

	public String getGridExportFeed() throws ComponentConfigException {
		return config.get(FEED_EXPORT_KEY, null);
	}

	public String getGridImportFeed() throws ComponentConfigException {
		return getKey(FEED_IMPORT_KEY);
	}

}
