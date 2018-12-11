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
package de.thebox.control.core.component.circ.pump;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class CirculationPumpConfig extends Configuration {

	protected final static String SECTION = "Circulation";

	protected final static String MIN_DELTA_TEMPERATURE_KEY = "circDeltaTempMin";
	protected final static double MIN_DELTA_TEMPERATURE_DEFAULT = 0.5;

	protected final static String MAX_REF_TEMPERATURE_KEY = "referenceTempMax";
	protected final static String MIN_REF_TEMPERATURE_KEY = "referenceTempMin";
	protected final static String REF_TEMPERATURE_KEY = "referenceTemp";

	protected final static String INTERVAL_KEY = "intervalMin";
	protected final static int INTERVAL_DEFAULT = 15;

	protected final static String PUMP_STATE_KEY = "pumpState";

	public CirculationPumpConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public boolean isEnabled() {
		String value = config.get(PUMP_STATE_KEY, null);
		if (value != null && !value.isEmpty()) {
			return true;
		}
		return false;
	}

	public double getDeltaTemperatureMin() {
		return config.getDouble(MIN_DELTA_TEMPERATURE_KEY, MIN_DELTA_TEMPERATURE_DEFAULT);
	}

	public double getReferenceTemperatureMax() throws ConfigurationException {
		return getDouble(MAX_REF_TEMPERATURE_KEY);
	}

	public double getReferenceTemperatureMin() throws ConfigurationException {
		return getDouble(MIN_REF_TEMPERATURE_KEY);
	}

	public String getReferenceTemperature() throws ConfigurationException {
		return getKey(REF_TEMPERATURE_KEY);
	}

	public String getState() throws ConfigurationException {
		return getKey(PUMP_STATE_KEY);
	}

	public int getIntervalMin() {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

}
