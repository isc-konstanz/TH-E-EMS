package org.the.ems.ctrl.tp;

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

@Component(
	
	scope = ServiceScope.BUNDLE,
	configurationPid = Control.PID+".2p",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TwoPointControl extends Control {
	private static final Logger logger = LoggerFactory.getLogger(TwoPointControl.class);

	@Configuration("temp_min")
	protected double temperatureMin;

	@Configuration("temp_max")
	protected double temperatureMax;

	@Configuration("temp")
	protected ChannelListener temperature;

	protected double temperatureValue = Double.NaN;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);

		temperature.registerValueListener(new TemperatureListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		temperature.deregister();
	}

	protected void onTemperatureHigh() {
		if (component instanceof HeatingService) {
			HeatingService heating = (HeatingService) component;
			try {
				if (heating.getState() == GeneratorState.STANDBY) {
					heating.start(new DoubleValue(heating.getMaxPower()));
				}
			} catch (EnergyManagementException e) {
				logger.warn("Unable to start heating \"{}\"", key);
			}
		}
	}

	protected void onTemperatureLow() {
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

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			temperatureValue = value.doubleValue();
			if (temperatureValue <= temperatureMin) {
				onTemperatureHigh();
			}
			else if (temperatureValue >= temperatureMax) {
				onTemperatureLow();
			}
		}
	}
}
