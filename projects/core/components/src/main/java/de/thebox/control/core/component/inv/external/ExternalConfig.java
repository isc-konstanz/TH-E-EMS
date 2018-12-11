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
package de.thebox.control.core.component.inv.external;

import java.util.prefs.Preferences;

import de.thebox.control.core.config.Configuration;
import de.thebox.control.core.config.ConfigurationException;

public class ExternalConfig extends Configuration {

	public final static String SECTION = "External";

	public final static String ENABLE_KEY = "enable";

	public final static String POWER_VIRTUAL_KEY = "virtualPower";
	public final static String POWER_ACTIVE_KEY = "activePower";

	public final static String REMOTE_PV_KEY = "pvPower";

	public ExternalConfig(Preferences configs) {
		super(configs);
	}

	@Override
	protected String getSectionKey() {
		return SECTION;
	}

	public String getEnabled() throws ConfigurationException {
		return getKey(ENABLE_KEY);
	}

	public String getVirtualPower() throws ConfigurationException {
		return getKey(POWER_VIRTUAL_KEY);
	}

	public String getActualPower() throws ConfigurationException {
		return getKey(POWER_ACTIVE_KEY);
	}

	public String getPvPower() throws ConfigurationException {
		return config.get(REMOTE_PV_KEY, null);
	}

}
