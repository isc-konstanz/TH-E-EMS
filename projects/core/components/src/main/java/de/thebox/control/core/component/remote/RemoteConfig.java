package de.thebox.control.core.component.remote;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class RemoteConfig extends Configuration {

	public final static String SECTION = "Remote";

	private final static String CONFIG_KEY = "config";
	private final static String CONFIG_DEFAULT = "/opt/emonmuc/conf/emoncms.conf";

	private final static String ADDRESS_KEY = "address";
	private final static String ADDRESS_DEFAULT = "http://localhost/emoncms/";
	private final static String AUTHORIZATION_KEY = "authorization";
	private final static String AUTHENTICATION_KEY = "authentication";
	private final static String MAX_THREADS_KEY = "maxThreads";
	private final static int MAX_THREADS_DEFAULT = 1;

	private final static String INTERVAL_KEY = "interval";
	private final static int INTERVAL_DEFAULT = 60000;

	private final Preferences serverConfig;

	public RemoteConfig(Preferences config) throws ConfigurationException {
		super(config);
		try {
			Ini ini = new Ini(getConfigFile());
			serverConfig = new IniPreferences(ini).node(SECTION);
			
		} catch (IOException e) {
			throw new ConfigurationException("Error opening emoncms configuration: " + e.getMessage());
		}
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	protected File getConfigFile() {
		return new File(config.get(CONFIG_KEY, CONFIG_DEFAULT));
	}

	public String getAddress() {
		return serverConfig.get(ADDRESS_KEY, ADDRESS_DEFAULT);
	}

	public String getAuthorization() {
		return serverConfig.get(AUTHORIZATION_KEY, null);
	}

	public String getAuthentication() {
		return serverConfig.get(AUTHENTICATION_KEY, null);
	}

	public int getMaxThreads() {
		return serverConfig.getInt(MAX_THREADS_KEY, MAX_THREADS_DEFAULT);
	}

	public int getInterval() {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT);
	}

	public boolean hasAuthentication() {
		if (serverConfig.get(AUTHENTICATION_KEY, null) != null && 
				serverConfig.get(AUTHORIZATION_KEY, null) != null) {
			return true;
		}
		return false;
	}
}
