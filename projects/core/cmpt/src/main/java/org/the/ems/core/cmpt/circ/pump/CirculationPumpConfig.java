/* 
 * Copyright 2016-18 ISC Konstanz
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
package org.the.ems.core.cmpt.circ.pump;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class CirculationPumpConfig extends Configurations {

	protected final static String SECTION = "Circulation";

	protected final static String STATE_KEY = "state";

	protected final static String INTERVAL_MIN_KEY = "interval_min";
	protected final static int INTERVAL_MIN_DEFAULT = 10;

	protected final static String TEMPERATURE_DELTA_MIN_KEY = "flow_temp_delta_min";
	protected final static double TEMPERATURE_DELTA_MIN_DEFAULT = 0.5;

	@Override
	protected String getSection() {
		return SECTION;
	}

	public boolean isEnabled() {
		return contains(STATE_KEY);
	}

	public String getState() throws ConfigurationException {
		return get(STATE_KEY);
	}

	public int getIntervalMin() {
		return prefs.getInt(INTERVAL_MIN_KEY, INTERVAL_MIN_DEFAULT)*60000;
	}

	public double getDeltaTemperatureMin() {
		return prefs.getDouble(TEMPERATURE_DELTA_MIN_KEY, TEMPERATURE_DELTA_MIN_DEFAULT);
	}

}
