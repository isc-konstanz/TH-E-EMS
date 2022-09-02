package org.the.ems.cmpt.inv.kaco;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.inv.Inverter;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;


@Component(
	scope = ServiceScope.BUNDLE,
	service = InverterService.class,
	configurationPid = InverterService.PID+".kaco",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BlueplanetHybrid extends Inverter<BlueplanetHyBat> {
	private static final Logger logger = LoggerFactory.getLogger(BlueplanetHybrid.class);

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration(mandatory = false)
	private Channel activePower;

	@Configuration("dc_power")
	private Channel inputPower;

	@Configuration
	private Channel setpointPower;
	private Value setpointControl = DoubleValue.zeroValue();

	private ValueListener socListener;

	@Configuration(mandatory = false)
	private double socHyst = 2.5;

	private double soc = 0;
	private boolean trickleCurrentFlag = false;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		socListener = new StateOfChargeListener();
		storage.registerStateOfChargeListener(socListener);
		
		inputPower.registerValueListener(new SetpointUpdater());
		if (activeError) {
			activePower.registerValueListener(new SetpointUpdater());
		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregisterStateOfChargeListener(socListener);
	}

	@Override
	protected void onSetpointUpdate(WriteContainer container, Value value) throws ComponentException {
		setpointControl = value;
		onSetpointUpdate(container);
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		double setpoint = setpointControl.doubleValue();
		double setpointLatest = 0;
		try {
			setpointLatest = -setpointPower.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
		}
		if (setpoint != 0 && activeError && activePower != null) {
			try {
				setpoint += setpointLatest - activePower.getLatestValue().doubleValue();
				
			} catch (InvalidValueException e) {
			}
		}
		
		if (trickleCurrentFlag) {
			if (setpoint < storage.getTricklePower()) {
				setpoint = storage.getTricklePower();
			}
		}
		
		if (setpoint != setpointLatest) {
			container.addDouble(setpointPower, -setpoint, setpointControl.getEpochMillis());
		}
		
	}

	private class SetpointUpdater implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			this.onSetpointChange();
		}

		protected void onSetpointChange() {
			try {
		        WriteContainer container = new WriteContainer();
		        doSetpointUpdate(container);
		        write(container);
		        
			} catch (EnergyManagementException e) {
				logger.warn("Error updating value: {}", e.getMessage());
			}
		}

		protected void doSetpointUpdate(WriteContainer container) throws ComponentException {
			onSetpointUpdate(container);
		};

	}

	private class StateOfChargeListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			soc = value.doubleValue();
			WriteContainer container = new WriteContainer();
			
			if (soc < storage.getMinStateOfCharge() && !trickleCurrentFlag) {
				trickleCurrentFlag = true;
				container.addDouble(setpointPower, -storage.getTricklePower(), System.currentTimeMillis());
				try {
					write(container);
				} catch (EnergyManagementException e) {
					logger.warn("Could not set trickle current: {}", e.getMessage());
				}
			}
			else if (soc >= storage.getMinStateOfCharge() + socHyst && trickleCurrentFlag) {
				trickleCurrentFlag = false;
				container.addDouble(setpointPower, -setpointControl.doubleValue(), System.currentTimeMillis());
				try {
					write(container);
				} catch (EnergyManagementException e) {
					logger.warn("Could not reset trickle current: {}", e.getMessage());
				}
				
			}
		}
	}

}
