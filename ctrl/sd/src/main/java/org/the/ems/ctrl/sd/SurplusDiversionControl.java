package org.the.ems.ctrl.sd;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.GeneratorState;
import org.the.ems.core.HeatingService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.Control;
import org.the.ems.ctrl.tp.TwoPointControl;

@Component(
	scope = ServiceScope.BUNDLE,
	configurationPid = Control.PID+".sd",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class SurplusDiversionControl extends TwoPointControl {
	private static final Logger logger = LoggerFactory.getLogger(SurplusDiversionControl.class);

	@Configuration(scale=1000, mandatory=false)
	protected double exportMin = Double.POSITIVE_INFINITY;

	@Configuration
	protected ChannelListener export;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		if (exportMin == Double.POSITIVE_INFINITY && component instanceof HeatingService) {
			exportMin = ((HeatingService) component).getMinPower();
		}
		export.registerValueListener(new PowerListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		export.deregister();
	}

	protected void onPowerSurplus(double value) {
		if (component instanceof HeatingService) {
			HeatingService heating = (HeatingService) component;
			try {
				if (temperatureValue <= temperatureMax) {
					// TODO: implement PID control for dynamic diversion
					heating.start(new DoubleValue(exportMin));
				}
			} catch (EnergyManagementException e) {
				logger.warn("Unable to start heating \"{}\"", key);
			}
		}
	}

	protected void onPowerShortage() {
		if (component instanceof HeatingService) {
			HeatingService heating = (HeatingService) component;
			try {
				if (heating.getState() == GeneratorState.RUNNING && heating.getRuntime() >= heating.getMinRuntime()) {
					heating.stop();
				}
			} catch (EnergyManagementException e) {
				logger.warn("Unable to stop heating \"{}\"", key);
			}
		}
	}

	private class PowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (value.doubleValue() >= exportMin) {
				onPowerSurplus(value.doubleValue());
			}
			else if (value.doubleValue() < 0) {
				onPowerShortage();
			}
		}
	}

}
