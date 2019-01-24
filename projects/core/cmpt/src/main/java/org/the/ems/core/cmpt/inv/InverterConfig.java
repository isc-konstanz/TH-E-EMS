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
package org.the.ems.core.cmpt.inv;

import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public class InverterConfig extends Configurations {

	public final static Value SETPOINT_DEFAULT = DoubleValue.emptyValue();
	protected final static String SETPOINT = "setpoint";

	protected final static String SETPOINT_MAX_KEY = "power_max";
	protected final static String SETPOINT_MIN_KEY = "power_min";

	protected final static String CONSUMPTION_POWER_KEY = "cons_power";

	public String getSetpoint() throws ConfigurationException {
		return get(SETPOINT);
	}

	public int getPowerMax() throws ConfigurationException {
		return (int) (getDouble(SETPOINT_MAX_KEY)*1000);
	}

	public int getPowerMin() throws ConfigurationException {
		return (int) (getDouble(SETPOINT_MIN_KEY)*1000);
	}

	public String getConsumptionPower() throws ConfigurationException {
		return get(CONSUMPTION_POWER_KEY);
	}

}
