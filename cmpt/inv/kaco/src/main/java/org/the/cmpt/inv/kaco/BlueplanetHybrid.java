package org.the.cmpt.inv.kaco;

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
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
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

	@Configuration("dc_power")
	private ChannelListener inputPower;

	@Configuration
	private ChannelListener activePower;

	@Configuration
	private Channel setpointPower;
	private Value setpointValue = DoubleValue.emptyValue();

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		storage.registerStateofCharge(new StateOfChargeListener());
		inputPower.registerValueListener(new SetpointUpdater());
		activePower.registerValueListener(new SetpointUpdater());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		storage.deregister();
		inputPower.deregister();
		activePower.deregister();
	}

	@Override
	protected void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		onSetpointUpdate(container, setpointValue = value);
	}

	private void onSetpointUpdate(WriteContainer container, Value setpointValue) throws ComponentException {
		double setpoint = 0;
		double setpointLatest = setpointPower.getLatestValue() != null ? 
				-setpointPower.getLatestValue().doubleValue() : 0;
		
		if (storage.getStateOfCharge().doubleValue() < storage.getMinStateOfCharge()) {
			setpoint = -Math.max(getInputPower().doubleValue(), storage.getTricklePower());
			if (logger.isDebugEnabled() && setpointValue.doubleValue() != 0) {
				logger.debug("Restricting charging power for Battery State of Charge of {}%", 
						storage.getStateOfCharge().doubleValue());
			}
		}
		else if (setpointValue.doubleValue() != 0) {
			setpoint = setpointLatest 
					+ setpointValue.doubleValue() - getActivePower().doubleValue();
		}
		if (setpoint != setpointLatest) {
			
			container.addDouble(setpointPower, -setpoint, setpointValue.getTime());
		}
	}

	private class SetpointUpdater implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			try {
		        WriteContainer container = new WriteContainer();
		        doSetpointUpdate(container);
		        doWrite(container);
		        
			} catch (EnergyManagementException e) {
				logger.warn("Error updating value: {}", e.getMessage());
			}
		}

		protected void doSetpointUpdate(WriteContainer container) throws ComponentException {
			onSetpointChanged(container, setpointValue);
		};

	}

	private class StateOfChargeListener extends SetpointUpdater {

		@Override
		public void onValueReceived(Value value) {
			double soc = value.doubleValue();
			if (soc < storage.getMinStateOfCharge()) {
				super.onValueReceived(value);
			}
		}
	}

}
