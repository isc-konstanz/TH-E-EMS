package de.thebox.control.component.inv.energydepot.objective;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private Channel objectiveSetpoint = null;
	private ChannelListener objectiveSetpointListener = null;
	private double setpoint = 0;

	private Emoncms emoncms = null;
	private ValueListener externalListener = null;
	private String externalPvFeed;
	private double external = 0;

	public Objective(ControlService control, Preferences prefs) throws ComponentException {
		ObjectiveConfig config = new ObjectiveConfig(prefs);
		try {
			this.objective = control.getChannel(config.getObjective());
			
			if (prefs.nodeExists(ExternalObjectiveConfig.SECTION)) {
				ExternalObjectiveConfig externalConfig = new ExternalObjectiveConfig(prefs);
				
				objectiveSetpoint = control.getChannel(externalConfig.getObjective());
				objectiveSetpointListener = registerObjectiveListener(objectiveSetpoint);
				
				if (prefs.nodeExists(EmoncmsConfig.SECTION)) {
					try {
						emoncms = new Emoncms(prefs);
						
						externalPvFeed = externalConfig.getPvFeed();
						externalListener = registerExternalPvListener(externalPvFeed);
						
					} catch (ControlException e) {
						throw new ComponentException("Error while activating emoncms listeners: " + e.getMessage());
					}
				}
			}
		} catch (BackingStoreException | UnknownChannelException e) {
			throw new ComponentConfigException("Invalid objective configuration: " + e.getMessage());
		}
	}

	private ChannelListener registerObjectiveListener(Channel channel) {
		ChannelListener stateListener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					setpoint = value.doubleValue();
					
					onObjectiveUpdate();
				}
			}
		};
		return stateListener;
	}

	private ValueListener registerExternalPvListener(String id) throws ControlException {
		ValueListener pvListener = new ValueListener() {
			
			@Override
			public void onValueReceived(Value value) {
				if (value != null) {
					external = value.doubleValue();
					
					onObjectiveUpdate();
				}
			}
		};
		emoncms.registerFeedListener(id, pvListener);
		
		return pvListener;
	}

	public void deactivate() {
		if (objectiveSetpointListener != null) {
			objectiveSetpointListener.deregister();
		}
		if (emoncms != null) {
			if (externalListener != null) {
				emoncms.deregisterFeedListener(externalPvFeed);
			}
			emoncms.deactivate();
		}
	}

	public void set(double value) throws ComponentException {
		if (value <= ObjectiveConfig.OBJECTIVE_MAX && value >= ObjectiveConfig.OBJECTIVE_MIN) {
			if (objective.getLatestValue().doubleValue() != value) {
					objective.writeValue(new DoubleValue(value));
			}
		}
		else throw new ComponentException("Inverter objective out of bounds: " + value);
	}

	public void reset() throws ComponentException {
		objective.writeValue(new DoubleValue(ObjectiveConfig.OBJECTIVE_DEFAULT));
	}

	private void onObjectiveUpdate() {
		double objective = this.setpoint - external;
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
