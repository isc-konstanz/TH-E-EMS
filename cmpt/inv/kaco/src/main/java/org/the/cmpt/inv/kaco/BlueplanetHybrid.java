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

	@Configuration(mandatory = false)
	private boolean activeError = false;

	@Configuration(mandatory = false)
	private ChannelListener activePower;

	@Configuration("dc_power")
	private ChannelListener inputPower;

	@Configuration
	private Channel setpointPower;
	private Value setpointValue = DoubleValue.emptyValue();

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		storage.registerStateofCharge(new StateOfChargeListener());
		inputPower.registerValueListener(new SetpointUpdater());
		if (activeError) {
			activePower.registerValueListener(new SetpointUpdater());
		}
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
		setpointValue = value;
		onSetpointUpdate(container);
	}

	private void onSetpointUpdate(WriteContainer container) throws ComponentException {
		double setpoint = setpointValue.doubleValue();
		double setpointLatest = setpointPower.getLatestValue() != null ? 
				-setpointPower.getLatestValue().doubleValue() : 0;
		
		if (setpoint != 0 && activeError && activePower != null) {
			setpoint += setpointLatest - activePower.getLatestValue().doubleValue();
		}
		if (storage.getStateOfCharge().doubleValue() < storage.getMinStateOfCharge() &&
				setpoint <= 0) {
			setpoint = Math.max(getInputPower().doubleValue(), storage.getTricklePower());
			if (logger.isDebugEnabled() && setpointValue.doubleValue() != 0) {
				logger.debug("Restricting charging power for Battery State of Charge of {}%", 
						storage.getStateOfCharge().doubleValue());
			}
		}
		if (setpoint != setpointLatest) {
			container.addDouble(setpointPower, -setpoint, setpointValue.getTime());
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
		        doWrite(container);
		        
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
			double soc = value.doubleValue();
			if (soc < storage.getMinStateOfCharge()) {
				onSetpointChange();
			}
		}
	}

}
