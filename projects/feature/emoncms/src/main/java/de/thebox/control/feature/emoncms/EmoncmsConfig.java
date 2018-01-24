package de.thebox.control.feature.emoncms;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.ini4j.InvalidFileFormatException;

public class EmoncmsConfig {

	private final static String SECTION = "Emoncms";

	private final static String ADDRESS_KEY = "address";
	private final static String ADDRESS_DEFAULT = "http://localhost/emoncms/";
	private final static String AUTHORIZATION_KEY = "authorization";
	private final static String AUTHENTICATION_KEY = "authentication";
	private final static String MAX_THREADS_KEY = "maxThreads";
	private final static int MAX_THREADS_DEFAULT = 1;

	private final Preferences configs;

	public EmoncmsConfig(String fileName) throws InvalidFileFormatException, IOException {
		Ini ini = new Ini(new File(fileName));
		configs = new IniPreferences(ini).node(SECTION);
	}

	public String getAddress() {
		return configs.get(ADDRESS_KEY, ADDRESS_DEFAULT);
	}

	public String getAuthorization() {
		return configs.get(AUTHORIZATION_KEY, null);
	}

	public String getAuthentication() {
		return configs.get(AUTHENTICATION_KEY, null);
	}

	public int getMaxThreads() {
		return configs.getInt(MAX_THREADS_KEY, MAX_THREADS_DEFAULT);
	}

	public boolean hasAuthentication() {
		if (configs.get(AUTHENTICATION_KEY, null) != null && 
				configs.get(AUTHORIZATION_KEY, null) != null) {
			return true;
		}
		return false;
	}
}
