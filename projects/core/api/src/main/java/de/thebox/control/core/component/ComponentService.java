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
package de.thebox.control.core.component;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.schedule.Schedule;

public interface ComponentService {

	public String getId();

	public default Component getType() {
		return Component.NONE;
	};

	public ComponentStatus getStatus();

	public void setStatus(ComponentStatus status) throws ControlException;

	public void bind(ControlService control) throws ControlException;

	public void activate(Preferences config) throws ControlException;

	public void reload() throws ControlException;

	public void set(Value value) throws ControlException;

	public void schedule(Schedule schedule) throws ControlException;

	public void deactivate();

}
