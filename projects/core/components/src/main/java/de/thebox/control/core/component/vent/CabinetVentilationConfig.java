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
package de.thebox.control.core.component.vent;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class CabinetVentilationConfig extends Configuration {

	public final static String SECTION = "Ventilation";

	public final static String VEMNTILATION_STATE_KEY = "ventState";

	public final static String TOP_TEMPERATURE_KEY = "cabinetTopTemp";
	public final static String CENTER_TEMPERATURE_KEY = "cabinetMidTemp";
	public final static String BOTTOM_TEMPERATURE_KEY = "cabinetBotTemp";

	public final static String TOLERANCE_KEY = "cabinetTempTolerance";
	public final static double TOLERANCE_DEFAULT = 0.5;

	public final static String MAX_TEMPERATURE_KEY = "cabinetTempMax";
	public final static double MAX_TEMPERATURE_DEFAULT = 50;

	public final static String INTERVAL_KEY = "intervalMin";
	public final static int INTERVAL_DEFAULT = 15;

	public CabinetVentilationConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getStateKey() throws ConfigurationException {
		return getKey(VEMNTILATION_STATE_KEY);
	}

	public String getTopTemperatureKey() throws ConfigurationException {
		return getKey(TOP_TEMPERATURE_KEY);
	}

	public String getCenterTemperatureKey() throws ConfigurationException {
		return getKey(CENTER_TEMPERATURE_KEY);
	}

	public String getBottomTemperatureKey() throws ConfigurationException {
		return getKey(BOTTOM_TEMPERATURE_KEY);
	}

	public double getTemperatureTolerance() {
		return config.getDouble(TOLERANCE_KEY, TOLERANCE_DEFAULT);
	}

	public double getTemperatureMax() {
		return config.getDouble(MAX_TEMPERATURE_KEY, MAX_TEMPERATURE_DEFAULT);
	}

	public int getIntervalMin() {
		return config.getInt(INTERVAL_KEY, INTERVAL_DEFAULT)*60000;
	}

}
