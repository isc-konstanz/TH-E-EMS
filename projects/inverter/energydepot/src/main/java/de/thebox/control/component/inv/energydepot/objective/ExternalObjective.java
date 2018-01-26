package de.thebox.control.component.inv.energydepot.objective;

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.component.inv.energydepot.EnergyDepotConst;
import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.data.Value;
import de.thebox.control.core.data.ValueListener;
import de.thebox.control.feature.emoncms.EmoncmsObserver;
import de.thebox.control.feature.emoncms.EmoncmsObserverException;


public class ExternalObjective {
	private final static Logger logger = LoggerFactory.getLogger(ExternalObjective.class);

	private final InverterService inverter;
	private final ControlService control;

	private final ValueListener objectiveListener;
	private String objectiveValue;
	private double objective = 0;

	private EmoncmsObserver observer = null;
	private ValueListener externalListener = null;
	private String externalFeed;
	private double external = 0;

	public ExternalObjective(InverterService inverter, ControlService control, Preferences configs, Preferences emoncms) throws ComponentException {
		this.inverter = inverter;
		this.control = control;
		
		objectiveValue = configs.get(ExternalObjectiveConst.OBJECTIVE_KEY, null);
		objectiveListener = registerObjectiveListener(objectiveValue);
		
		try {
			if (emoncms != null) {
				observer = new EmoncmsObserver(emoncms);
				
				externalFeed = configs.get(ExternalObjectiveConst.FEED_KEY, null);
				externalListener = registerExternalPvListener(externalFeed);
			}
		} catch (EmoncmsObserverException e) {
			throw new ComponentException("Error while activating emoncms observer: " + e.getMessage());
		}
	}

	private ValueListener registerObjectiveListener(String id) throws ComponentConfigException {
		if (id != null) {
			ValueListener stateListener = new ValueListener() {
				
				@Override
				public void onValueReceived(Value value) {
					if (value != null) {
						objective = value.doubleValue();
						
						onObjectiveUpdate();
					}
				}
			};
			control.registerValueListener(id, stateListener);
			
			return stateListener;
		}
		throw new ComponentConfigException("Unable to find configured state value");
	}

	private ValueListener registerExternalPvListener(String id) throws ComponentConfigException, EmoncmsObserverException {
		if (id != null) {
			ValueListener pvListener = new ValueListener() {
				
				@Override
				public void onValueReceived(Value value) {
					if (value != null) {
						external = value.doubleValue();
						
						onObjectiveUpdate();
					}
				}
			};
			observer.registerFeedListener(id, pvListener);
			
			return pvListener;
		}
		throw new ComponentConfigException("Unable to find configured state value");
	}

	public void deactivate() {
		if (objectiveListener != null) {
			control.deregisterValueListener(objectiveValue, objectiveListener);
		}
		if (observer != null) {
			if (externalListener != null) {
				observer.deregisterFeedListener(externalFeed);
			}
			observer.deactivate();
		}
	}
	
	private void onObjectiveUpdate() {
		double objective = this.objective - external;
		if (objective > EnergyDepotConst.OBJECTIVE_MAX) {
			objective = EnergyDepotConst.OBJECTIVE_MAX;
		}
		else if (objective < EnergyDepotConst.OBJECTIVE_MIN) {
			objective = EnergyDepotConst.OBJECTIVE_MIN;
		}
		try {
			inverter.setObjective(objective);
			
		} catch (ComponentException e) {
			logger.warn("Error while setting inverter objective to {}: {}", objective, e.getMessage());
		}
	}
}
