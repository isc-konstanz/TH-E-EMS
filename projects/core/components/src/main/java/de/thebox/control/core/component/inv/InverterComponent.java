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
package de.thebox.control.core.component.inv;

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ComponentImpl;
import de.thebox.control.core.component.ComponentWriteContainer;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.component.MaintenanceException;
import de.thebox.control.core.component.inv.consumption.Consumption;
import de.thebox.control.core.component.inv.external.External;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;

public abstract class InverterComponent extends ComponentImpl implements InverterService {
	private final static Logger logger = LoggerFactory.getLogger(InverterComponent.class);

	protected External external;
	protected Consumption consumption;

	protected ChannelListener objective;
	protected int objectiveMax;
	protected int objectiveMin;
	protected Value objectiveLast = DoubleValue.emptyValue();

	protected Channel batteryState;
	protected double batteryStateMin;

	protected volatile boolean maintenance = false;

	protected class ObjectiveListener extends ChannelListener {
		public ObjectiveListener(Channel channel) {
			super(channel);
		}

		@Override
		public void onValueReceived(Value objective) {
			onUpdate();
		}
	}

	@Override
	public void activate(Preferences prefs) throws ControlException {
		InverterConfig config = new InverterConfig(prefs);
		objective = new ObjectiveListener(control.getChannel(config.getObjective()));
		consumption = new Consumption(this, control, config, prefs);
		external = new External(control, prefs);
	}

	@Override
	public void deactivate() {
		external.deactivate();
		consumption.deactivate();
		objective.deregister();
	}

	@Override
	protected void maintenance(boolean enabled) throws ControlException {
		if (enabled) {
			set(InverterConfig.OBJECTIVE_DEFAULT);
		}
		external.setEnabled(!enabled);
		
		maintenance = enabled;
	}

	@Override
	public void set(Value value) throws ControlException {
		objective.getChannel().setLatestValue(value);
	}

	@Override
	public void build(ComponentWriteContainer container, Value value) throws ComponentException {
		if (maintenance) {
			throw new MaintenanceException();
		}
		if (value.doubleValue() > objectiveMax || value.doubleValue() < objectiveMin) {
			throw new ComponentException("Inverter objective out of bounds: " + value);
		}
		double objective = value.doubleValue();
		if (external.isEnabled()) {
			objective -= external.getPv().doubleValue();
		}
		
		if (objective > objectiveMax) {
			objective = objectiveMax;
		}
		else if (objective < objectiveMin) {
			objective = objectiveMin;
		}
		if (objective == objectiveLast.doubleValue()) {
			// Do Nothing
			return;
		}
		else if (objective > 0) {
			Value state = batteryState.getLatestValue();
			if (state != null && state.doubleValue() < batteryStateMin) {
				try {
					set(InverterConfig.OBJECTIVE_DEFAULT);
					
				} catch (ControlException e) {
				}
				throw new ComponentException("Battery State of Charge below boundaries. Export temporarily disabled.");
			}
		}
		objective(container, new DoubleValue(objective, value.getTime()));
	}

	@Override
	public void onUpdate() {
		ComponentWriteContainer container = new ComponentWriteContainer();
		try {
			build(container, objectiveLast);
			
		} catch (MaintenanceException e) {
			logger.debug("Skipped writing values for component \"{}\" due to maintenance", getId());
		} catch (ControlException e) {
			logger.debug("Unable to updating inverter objective: {}", e.getMessage());
		}
		if (container.size() < 1) {
			return;
		}
		
		for (Channel channel : container.keySet()) {
			channel.write(container.get(channel));
		}
	}

}
