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
package org.the.ems.core.cmpt.hp;

import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatPumpService;
import org.the.ems.core.cmpt.GenericComponent;
import org.the.ems.core.cmpt.circ.Circulation;
import org.the.ems.core.cmpt.circ.pump.CirculationPump;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;

public abstract class HeatPumpComponent extends GenericComponent implements HeatPumpService {

	protected Circulation circulation;
	protected CirculationPump circulationPump;

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		circulation = new Circulation(manager, configs);
		circulationPump = new CirculationPump(manager, circulation, configs);
	}

	@Override
	public void onDeactivate() {
		circulationPump.deactivate();
		circulation.deactivate();
	}

	@Override
	protected void onMaintenance(boolean enabled) throws EnergyManagementException {
		circulationPump.setEnabled(!enabled);
	}

	@Override
	public void onUpdate() {
		// Do nothing for now
	}

	@Override
	protected void onSet(ComponentWriteContainer container, Value value) throws ComponentException {
		if (value.doubleValue() > 0) {
			start(container, value);
		}
		else if (value.doubleValue() == 0) {
			stop(container, value.getTime());
		}
		throw new ComponentException("Invalid negative power values passed to set component");
	}

	public abstract void start(ComponentWriteContainer container, Value value) throws ComponentException;

	public abstract void stop(ComponentWriteContainer container, Long time) throws ComponentException;

}
