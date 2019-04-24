package org.the.cmpt.ctrl.mpc.service;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class GridServiceConfig extends Configurations {

	public final static String SERVICE_KEY = "service";

	public boolean hasService() {
		return contains(SERVICE_KEY);
	}

	public String getService() throws ConfigurationException {
		return get(SERVICE_KEY);
	}

}
