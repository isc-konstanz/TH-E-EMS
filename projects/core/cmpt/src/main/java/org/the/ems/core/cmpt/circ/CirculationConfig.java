/* 
 * Copyright 2016-19 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.the.ems.core.cmpt.circ;

import org.the.ems.core.config.Configurations;
import org.the.ems.core.config.ConfigurationException;

public class CirculationConfig extends Configurations {

	protected final static String SECTION = "Circulation";

	// The density of the flow medium. Default is 1 of water.
	protected final static String FLOW_DENSITY_KEY = "flow_density";
	protected final static double FLOW_DENSITY_DEFAULT = 1;

	// The specific heat capacity of the flow medium. Default is 4.1813 of water.
	protected final static String FLOW_HEAT_SPECIFIC_KEY = "flow_heat_specific";
	protected final static double FLOW_HEAT_SPECIFIC_DEFAULT = 4.1813;

	// The channel key of the counter in liters
	protected final static String FLOW_COUNTER_KEY = "flow_counter";
	protected final static String FLOW_ENERGY_KEY = "flow_energy";

	protected final static String TEMPERATURE_DELTA_KEY = "flow_temp_delta";
	protected final static String TEMPERATURE_OUT_KEY = "flow_temp_out";
	protected final static String TEMPERATURE_IN_KEY = "flow_temp_in";

	@Override
	protected String getSection() {
		return SECTION;
	}

	public double getFlowDensity() throws ConfigurationException {
		return getDouble(FLOW_DENSITY_KEY, FLOW_DENSITY_DEFAULT);
	}

	public double getFlowSpecificHeat() throws ConfigurationException {
		return getDouble(FLOW_HEAT_SPECIFIC_KEY, FLOW_HEAT_SPECIFIC_DEFAULT);
	}

	public String getFlowEnergy() throws ConfigurationException {
		return get(FLOW_ENERGY_KEY);
	}

	public String getFlowCounter() throws ConfigurationException {
		return get(FLOW_COUNTER_KEY);
	}

	public String getDeltaTemperature() throws ConfigurationException {
		return get(TEMPERATURE_DELTA_KEY);
	}

	public String getInTemperature() throws ConfigurationException {
		return get(TEMPERATURE_IN_KEY);
	}

	public String getOutTemperature() throws ConfigurationException {
		return get(TEMPERATURE_OUT_KEY);
	}

}
