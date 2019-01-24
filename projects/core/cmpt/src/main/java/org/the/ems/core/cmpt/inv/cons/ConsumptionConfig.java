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
package org.the.ems.core.cmpt.inv.cons;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class ConsumptionConfig extends Configurations {

	protected final static String SECTION = "Consumption";

	protected final static String AC_POWER_KEY = "ac_power";
	protected final static String DC_POWER_KEY = "dc_power";
	protected final static String DC1_POWER_KEY = "dc1_power";
	protected final static String DC2_POWER_KEY = "dc2_power";

	protected final static String ESS_POWER_KEY = "ess_power";

	@Override
	protected String getSection() {
		return SECTION;
	}

	public String getAcPower() throws ConfigurationException {
		return get(AC_POWER_KEY);
	}

	public String getDcPower() throws ConfigurationException {
		return get(DC_POWER_KEY);
	}

	public String getDcPower1() throws ConfigurationException {
		return get(DC1_POWER_KEY);
	}

	public String getDcPower2() throws ConfigurationException {
		return get(DC2_POWER_KEY);
	}

	public boolean hasDcPower2() {
		return contains(DC2_POWER_KEY);
	}

	public String getStoragePower() throws ConfigurationException {
		return get(ESS_POWER_KEY);
	}

}
