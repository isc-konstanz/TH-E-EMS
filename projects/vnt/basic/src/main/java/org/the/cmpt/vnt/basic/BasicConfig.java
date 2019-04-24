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
package org.the.cmpt.vnt.basic;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class BasicConfig extends Configurations {

	public final static String SECTION = "Ventilation";

	public final static String STATE_KEY = "state";

	public final static String INTERVAL_MIN_KEY = "interval_min";
	public final static int INTERVAL_MIN_DEFAULT = 10;

	public final static String TEMPERATURE_TOP_KEY = "case_temp_top";
	public final static String TEMPERATURE_CENTER_KEY = "case_temp_mid";
	public final static String TEMPERATURE_BOTTOM_KEY = "case_temp_bot";

	public final static String TEMPERATURE_TOLERANCE_KEY = "case_temp_tolerance";
	public final static double TEMPERATURE_TOLERANCE_DEFAULT = 0.5;

	public final static String TEMPERATURE_MAX_KEY = "case_temp_max";
	public final static double TEMPERATURE_MAX_DEFAULT = 50;

	@Override
	protected String getSection() {
		return SECTION;
	}

	public String getStateKey() throws ConfigurationException {
		return get(STATE_KEY);
	}

	public int getIntervalMin() {
		return prefs.getInt(INTERVAL_MIN_KEY, INTERVAL_MIN_DEFAULT)*60000;
	}

	public String getTopTemperatureKey() throws ConfigurationException {
		return get(TEMPERATURE_TOP_KEY);
	}

	public String getCenterTemperatureKey() throws ConfigurationException {
		return get(TEMPERATURE_CENTER_KEY);
	}

	public String getBottomTemperatureKey() throws ConfigurationException {
		return get(TEMPERATURE_BOTTOM_KEY);
	}

	public double getTemperatureTolerance() {
		return prefs.getDouble(TEMPERATURE_TOLERANCE_KEY, TEMPERATURE_TOLERANCE_DEFAULT);
	}

	public double getTemperatureMax() {
		return prefs.getDouble(TEMPERATURE_MAX_KEY, TEMPERATURE_MAX_DEFAULT);
	}

}
