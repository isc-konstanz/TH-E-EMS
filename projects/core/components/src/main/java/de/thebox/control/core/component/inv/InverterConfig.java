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
package de.thebox.control.core.component.inv;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

public class InverterConfig extends Configuration {

	public final static String SECTION = "Inverter";

	public final static String OBJECTIVE = "objective";
	public final static String OBJECTIVE_MAX_KEY = "powerMax";
	public final static String OBJECTIVE_MIN_KEY = "powerMin";
	public final static Value OBJECTIVE_DEFAULT = DoubleValue.emptyValue();

	public final static String CONSUMPTION_POWER_KEY = "consPower";

	public final static String BATTERY_SOC_KEY = "batState";
	public final static String BATTERY_SOC_MIN_KEY = "batStateMin";
	public final static double BATTERY_SOC_MIN_DEFAULT = 10;

	public InverterConfig(Preferences config) {
		super(config);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getObjective() throws ConfigurationException {
		return getKey(OBJECTIVE);
	}

	public int getPowerMax() throws ConfigurationException {
		return (int) (getDouble(OBJECTIVE_MAX_KEY)*1000);
	}

	public int getPowerMin() throws ConfigurationException {
		return (int) (getDouble(OBJECTIVE_MIN_KEY)*1000);
	}

	public String getConsumptionPower() throws ConfigurationException {
		return getKey(CONSUMPTION_POWER_KEY);
	}

	public String getBatterySoC() throws ConfigurationException {
		return getKey(BATTERY_SOC_KEY);
	}

	public double getBatteryStateMin() {
		return config.getDouble(BATTERY_SOC_MIN_KEY, BATTERY_SOC_MIN_DEFAULT);
	}

}
