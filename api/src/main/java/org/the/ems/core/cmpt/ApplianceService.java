/* 
 * Copyright 2016-20 ISC Konstanz
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
package org.the.ems.core.cmpt;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
import org.the.ems.core.RunnableService;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

public interface ApplianceService extends RunnableService {

	static final String PID = "org.the.ems.cmpt.apl";

	@Override
	public default ComponentType getType() {
		return ComponentType.APPLIANCE;
	};

	/*
	 * Get the default value with which a component will be started with.
	 */
	@Override
	public default Value getStartValue(long time) {
		return new DoubleValue(getStartPower(), time);
	}

	/*
	 * Get the default power with which a component will be started with in watts [W].
	 */
	public double getStartPower();

	/*
	 * Get the minimum power in watts [W].
	 */
	public double getMinPower();

	/*
	 * Get the maximum power in watts [W].
	 */
	public double getMaxPower();

	/*
	 * Get the consumed energy in kilowatt hours [kWh].
	 */
	public Value getEnergy() throws ComponentException;

	/*
	 * Get the consumption power in watts [W].
	 */
	public Value getPower() throws ComponentException;

}
