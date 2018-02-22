package de.thebox.control.component.inv.energydepot.objective;

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.inv.energydepot.consumption.Consumption;
import de.thebox.control.component.inv.energydepot.external.External;
import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.config.ConfigurationException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;

public class Objective {
	private final static Logger logger = LoggerFactory.getLogger(Objective.class);

	private Channel objective;
	private Channel batteryState;
	private final double batteryStateMin;
	private double setpoint = 0;

	private ValueListener consumptionListener;
	private Value consumption = DoubleValue.emptyValue();

	private Channel virtualObjective;
	private ChannelListener virtualObjectiveListener;
	private External external;

	public Objective(ControlService control, Consumption consumption, Preferences prefs) throws ControlException {
		ObjectiveConfig config = new ObjectiveConfig(prefs);
		try {
			objective = control.getChannel(config.getObjective());
			virtualObjective = control.getChannel(config.getVirtualObjective());
			virtualObjectiveListener = registerObjectiveListener(virtualObjective);
			
			batteryState = control.getChannel(config.getBatterySoC());
			batteryStateMin = config.getBatteryStateMin();
			
			consumptionListener = registerConsumptionListener(consumption);
			
			external = new External(control, prefs);
			
		} catch (UnknownChannelException e) {
			throw new ConfigurationException("Invalid objective configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerObjectiveListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					setpoint = value.doubleValue();
					onObjectiveUpdate();
				}
			}
		};
		return listener;
	}

	private ValueListener registerConsumptionListener(Consumption context) {
		ValueListener listener = new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					consumption = value;
					onObjectiveUpdate();
				}
			}
		};
		context.register(listener);
		
		return listener;
	}

	public void deactivate(Consumption consumption) {
		if (virtualObjectiveListener != null) {
			virtualObjectiveListener.deregister();
		}
		if (consumption != null && consumptionListener != null) {
			consumption.deregister(consumptionListener);
		}
	}

	public void setSetpoint(Value setpoint) {
		virtualObjective.writeValue(setpoint);
	}

	public void set(double value) throws ComponentException {
		if (value > ObjectiveConfig.OBJECTIVE_MAX || value < ObjectiveConfig.OBJECTIVE_MIN) {
			throw new ComponentException("Inverter objective out of bounds: " + value);
		}
		Value state = batteryState.getLatestValue();
		if (state != null && state.doubleValue() < batteryStateMin) {
			reset();
		}
		else if (objective.getLatestValue().doubleValue() != value) {
			objective.writeValue(new DoubleValue(value));
		}
	}

	public void reset() throws ComponentException {
		objective.writeValue(new DoubleValue(ObjectiveConfig.OBJECTIVE_DEFAULT));
	}

	private void onObjectiveUpdate() {
		double objective = setpoint;
		
		if (external.isEnabled()) {
			external.update(consumption.getTimestamp());
			objective += (consumption.doubleValue() - external.getPv().doubleValue());
		}
		else if (setpoint > 0) {
			objective += consumption.doubleValue();
		}
		
		if (objective > ObjectiveConfig.OBJECTIVE_MAX) {
			logger.warn("Inverter objective out of bounds and will be adjusted: {}", objective);
			objective = ObjectiveConfig.OBJECTIVE_MAX;
		}
		else if (objective < ObjectiveConfig.OBJECTIVE_MIN) {
			logger.warn("Inverter objective out of bounds and will be adjusted: {}", objective);
			objective = ObjectiveConfig.OBJECTIVE_MIN;
		}
		try {
			set(objective);
			
		} catch (ComponentException e) {
			logger.warn("Error while setting inverter objective to {}: {}", objective, e.getMessage());
		}
	}
}
