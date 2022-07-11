/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
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

import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;

public interface VentilationService extends ComponentService {

	public static final String PID = "org.the.ems.cmpt.vnt";

	@Override
	public default ComponentType getType() {
		return ComponentType.VENTILATION;
	};

	/*
	 * Start the ventilation.
	 * 
	 * @throws EnergyManagementException if any kind of error occurs starting the ventilation
	 */
	public void start() throws EnergyManagementException;

	/*
	 * Stop the ventilation.
	 * 
	 * @throws EnergyManagementException if any kind of error occurs stopping the ventilation
	 */
	public void stop() throws EnergyManagementException;

}
