/* 
 * Copyright 2016-19 ISC Konstanz
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
package org.the.ems.core;

import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.Schedule;

public interface ComponentService {

	public String getId();

	public default ComponentType getType() {
		return ComponentType.NONE;
	};

	public ComponentStatus getStatus();

	public void setStatus(ComponentStatus status) throws EnergyManagementException;

	public void onBind(ContentManagementService control) throws EnergyManagementException;

	public void onActivate(Configurations config) throws EnergyManagementException;

	public void onReload(Configurations config) throws EnergyManagementException;

	public void onDeactivate();

	public void set(Value value) throws EnergyManagementException, UnsupportedOperationException;

	public void schedule(Schedule schedule) throws EnergyManagementException, UnsupportedOperationException;

}
