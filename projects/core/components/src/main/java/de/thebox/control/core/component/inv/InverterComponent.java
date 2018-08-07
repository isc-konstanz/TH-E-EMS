package de.thebox.control.core.component.inv;

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ComponentImpl;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.component.MaintenanceException;
import de.thebox.control.core.component.inv.consumption.Consumption;
import de.thebox.control.core.component.inv.external.External;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelValues;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;

public abstract class InverterComponent extends ComponentImpl implements InverterService {
	private final static Logger logger = LoggerFactory.getLogger(InverterComponent.class);

	protected External external;
	protected Consumption consumption;
	protected Value consumptionLast = DoubleValue.emptyValue();

	protected int objectiveMax;
	protected int objectiveMin;
	protected Value objectiveLast = DoubleValue.emptyValue();

	protected double batteryStateMin;
	protected Channel batteryState;

	protected volatile boolean maintenance = false;

	@Override
	public void activate(Preferences config) throws ControlException {
		external = new External(control, config);
		consumption = new Consumption(control, config);
		consumption.register(new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				try {
					consumptionLast = value;
					update();
					
				} catch (ControlException e) {
					logger.debug("Unable to updating inverter objective: {}", e.getMessage());
				}
			}
		});
	}

	@Override
	public void deactivate() {
		consumption.deactivate();
		external.deactivate();
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
	public ChannelValues build(Value value) throws ComponentException {
		if (maintenance) {
			throw new MaintenanceException();
		}
		value = process(value);
		
		if (value.doubleValue() > objectiveMax || value.doubleValue() < objectiveMin) {
			throw new ComponentException("Inverter objective out of bounds: " + value);
		}
		else if (value.doubleValue() == objectiveLast.doubleValue()) {
			// Do Nothing
			return new ChannelValues();
		}
		else if (value.doubleValue() > 0) {
			Value state = batteryState.getLatestValue();
			if (state != null && state.doubleValue() < batteryStateMin) {
				try {
					set(InverterConfig.OBJECTIVE_DEFAULT);
					
				} catch (ControlException e) {
				}
				throw new ComponentException("Battery State of Charge below boundaries. Export temporarily disabled.");
			}
		}
		objectiveLast = value;
		
		return objective(value);
	}

	protected abstract void update() throws ControlException;

	protected Value process(Value value) throws ComponentException {
		double result = value.doubleValue();
		if (external.isEnabled()) {
			result -= external.getPv().doubleValue();
		}
		return new DoubleValue(result, value.getTime());
	}

}
