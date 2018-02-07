package de.thebox.control.component.inv.energydepot.objective;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.inv.energydepot.consumption.Consumption;
import de.thebox.control.core.ControlException;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.ChannelListener;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;
import de.thebox.control.feature.emoncms.Emoncms;
import de.thebox.control.feature.emoncms.EmoncmsConfig;


public class Objective {
	private final static Logger logger = LoggerFactory.getLogger(Objective.class);

	private Channel objective;
	private Channel batteryState;
	private final double batteryStateMin;
	private double setpoint = 0;

	private ValueListener consumptionListener;
	private Value consumption = DoubleValue.emptyValue();

	private Emoncms emoncms = null;
	private boolean externalPvEnabled = false;
	private String externalPvFeed = null;
	private ValueListener externalListener = null;
	private double externalPv = 0;

	private Channel actualPower = null;
	private Channel virtualPower = null;
	private Channel virtualObjective = null;
	private ChannelListener virtualObjectiveListener = null;

	public Objective(ControlService control, Consumption consumption, Preferences prefs) throws ComponentException {
		ObjectiveConfig config = new ObjectiveConfig(prefs);
		try {
			objective = control.getChannel(config.getObjective());
			batteryState = control.getChannel(config.getBatterySoC());
			batteryStateMin = config.getBatteryStateMin();
			consumptionListener = registerConsumptionListener(consumption);
			
			if (prefs.nodeExists(ExternalObjectiveConfig.SECTION) && prefs.nodeExists(EmoncmsConfig.SECTION)) {
				ExternalObjectiveConfig externalConfig = new ExternalObjectiveConfig(prefs);
				try {
					emoncms = new Emoncms(prefs);
					
					externalPvEnabled = true;
					externalPvFeed = externalConfig.getPvFeed();
					externalListener = registerExternalPvListener(externalPvFeed);
					
				} catch (ControlException e) {
					throw new ComponentException("Error while activating emoncms listeners: " + e.getMessage());
				}
				actualPower = control.getChannel(externalConfig.getActualPower());
				virtualPower = control.getChannel(externalConfig.getVirtualPower());
				virtualObjective = control.getChannel(externalConfig.getVirtualObjective());
				virtualObjectiveListener = registerObjectiveListener(virtualObjective);
			}
		} catch (BackingStoreException | UnknownChannelException e) {
			throw new ComponentConfigException("Invalid objective configuration: " + e.getMessage());
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

	private ValueListener registerExternalPvListener(String id) throws ControlException {
		ValueListener listener = new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					externalPv = value.doubleValue();
					onObjectiveUpdate();
				}
			}
		};
		emoncms.registerFeedListener(id, listener);
		
		return listener;
	}

	public void deactivate(Consumption consumption) {
		if (virtualObjectiveListener != null) {
			virtualObjectiveListener.deregister();
		}
		if (consumption != null && consumptionListener != null) {
			consumption.deregister(consumptionListener);
		}
		if (emoncms != null) {
			if (externalListener != null) {
				emoncms.deregisterFeedListener(externalPvFeed);
			}
			emoncms.deactivate();
		}
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
		
		if (externalPvEnabled) {
			objective += (consumption.doubleValue() - externalPv);
			
			Value actualValue = actualPower.getLatestValue();
			if (actualValue != null && actualValue.getTimestamp() == consumption.getTimestamp()) {
				Value virtualValue = new DoubleValue(actualValue.doubleValue() - externalPv, actualValue.getTimestamp());
				virtualPower.setLatestValue(virtualValue);
			}
		}
		else if (setpoint > 0) {
			objective += consumption.doubleValue();
		}
		
		if (objective > ObjectiveConfig.OBJECTIVE_MAX) {
			objective = ObjectiveConfig.OBJECTIVE_MAX;
		}
		else if (objective < ObjectiveConfig.OBJECTIVE_MIN) {
			objective = ObjectiveConfig.OBJECTIVE_MIN;
		}
		try {
			set(objective);
			
		} catch (ComponentException e) {
			logger.warn("Error while setting inverter objective to {}: {}", objective, e.getMessage());
		}
	}
}
