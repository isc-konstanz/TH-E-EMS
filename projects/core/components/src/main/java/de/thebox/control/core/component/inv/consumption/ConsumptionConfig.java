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
package de.thebox.control.core.component.inv.consumption;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class ConsumptionConfig extends Configuration {

	public final static String SECTION = "Consumption";

	public final static String BATTERY_POWER_KEY = "batPower";
	public final static String AC_POWER_KEY = "acPower";
	public final static String DC_POWER_KEY = "dcPower";
	public final static String DC1_POWER_KEY = "dc1Power";
	public final static String DC2_POWER_KEY = "dc2Power";

	public ConsumptionConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getBatteryPower() throws ConfigurationException {
		return getKey(BATTERY_POWER_KEY);
	}

	public String getAcPower() throws ConfigurationException {
		return getKey(AC_POWER_KEY);
	}

	public String getDcPower() throws ConfigurationException {
		return getKey(DC_POWER_KEY);
	}

	public String getDcPower1() throws ConfigurationException {
		return getKey(DC1_POWER_KEY);
	}

	public String getDcPower2() throws ConfigurationException {
		return getKey(DC2_POWER_KEY);
	}

	public boolean hasDcPower2() {
		String value = config.get(DC2_POWER_KEY, null);
		if (value != null && !value.isEmpty()) {
			return true;
		}
		return false;
	}

}
