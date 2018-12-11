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
package de.thebox.control.core.component.hp;

import java.util.prefs.Preferences;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.CabinetService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ComponentImpl;
import de.thebox.control.core.component.ComponentWriteContainer;
import de.thebox.control.core.component.HeatPumpService;
import de.thebox.control.core.component.circ.Circulation;
import de.thebox.control.core.component.circ.pump.CirculationPump;
import de.thebox.control.core.component.vent.CabinetVentilation;
import de.thebox.control.core.data.Value;

public abstract class HeatPumpComponent extends ComponentImpl implements CabinetService, HeatPumpService {

	private Circulation circulation;
	private CirculationPump circulationPump;
	private CabinetVentilation ventilation;

	protected volatile boolean maintenance = false;

	@Override
	public void activate(Preferences config) throws ControlException {
		circulation = new Circulation(control, config);
		circulationPump = new CirculationPump(control, circulation, config);
		ventilation = new CabinetVentilation(control, config);
	}

	@Override
	public void deactivate() {
		circulationPump.deactivate();
		circulation.deactivate();
		ventilation.deactivate();
	}

	@Override
	protected void maintenance(boolean enabled) throws ControlException {
		ventilation.setEnabled(!enabled);
		maintenance = enabled;
	}

	@Override
	public void ventilation(boolean enabled) throws ComponentException {
		if (enabled) {
			ventilation.start();
		}
		else {
			ventilation.stop();
		}
	}

	@Override
	protected void build(ComponentWriteContainer container, Value value) throws ControlException {
		if (value.doubleValue() == 0) {
			stop(container);
		}
		else if (value.doubleValue() > 0) {
			start(container, value);
		}
		throw new ComponentException("Invalid negative power values passed to set component");
	}

	@Override
	public void onUpdate() {
		// Do nothing for now
	}

}
