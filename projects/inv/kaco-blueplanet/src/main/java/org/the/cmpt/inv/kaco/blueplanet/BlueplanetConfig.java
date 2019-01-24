package org.the.cmpt.inv.kaco.blueplanet;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class BlueplanetConfig extends Configurations {

	protected final static String SECTION = "Storage";

	protected final static String ESS_SETPOINT_KEY = "ess_setpoint";

	protected final static String ESS_SOC_KEY = "ess_soc";
	protected final static String ESS_SOC_MIN_KEY = "ess_soc_min";
	protected final static double ESS_SOC_MIN_DEFAULT = 10;

	protected final static String ESS_CAPACITY_KEY = "ess_capacity";

	@Override
	protected String getSection() {
		return SECTION;
	}

	public String getChargeSetpoint() throws ConfigurationException {
		return get(ESS_SETPOINT_KEY);
	}

	public String getStateOfCharge() throws ConfigurationException {
		return get(ESS_SOC_KEY);
	}

	public double getStateMin() {
		return getDouble(ESS_SOC_MIN_KEY, ESS_SOC_MIN_DEFAULT);
	}

	public double getCapacity() throws ConfigurationException {
		return getDouble(ESS_CAPACITY_KEY);
	}

}
