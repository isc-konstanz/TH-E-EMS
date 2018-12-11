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
package de.thebox.control.core.component.circ;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class CirculationConfig extends Configuration {

	protected final static String SECTION = "Circulation";

	protected final static String DELTA_TEMPERATURE_KEY = "circDeltaTemp";
	protected final static String OUT_TEMPERATURE_KEY = "circOutTemp";
	protected final static String IN_TEMPERATURE_KEY = "circInTemp";

	public CirculationConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getDeltaTemperature() throws ConfigurationException {
		return getKey(DELTA_TEMPERATURE_KEY);
	}

	public String getInTemperature() throws ConfigurationException {
		return getKey(IN_TEMPERATURE_KEY);
	}

	public String getOutTemperature() throws ConfigurationException {
		return getKey(OUT_TEMPERATURE_KEY);
	}

}
