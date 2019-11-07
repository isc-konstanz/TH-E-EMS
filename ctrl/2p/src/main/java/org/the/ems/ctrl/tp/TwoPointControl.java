package org.the.ems.ctrl.tp;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.Control;

@Component(
	
	scope = ServiceScope.BUNDLE,
	configurationPid = Control.PID+".2p",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TwoPointControl extends Control {

	@Configuration("temp_min")
	private double temperatureMin;

	@Configuration("temp_max")
	private double temperatureMax;

	@Configuration("temp")
	private ChannelListener temperature;

	private double temperatureValue = Double.NaN;

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

	@Override
	protected boolean checkStart(HeatingService heating) {
		return checkMinTemperature();
	}

	protected boolean checkMinTemperature() {
		return temperatureValue <= temperatureMax;
	}

	@Override
	protected boolean checkStop(HeatingService heating) {
		return checkMaxTemperature();
	}

	protected boolean checkMaxTemperature() {
		return temperatureValue >= temperatureMax;
	}

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			temperatureValue = value.doubleValue();
			onControlHeating();
		}
	}

}
