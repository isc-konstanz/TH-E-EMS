package org.the.ems.ctrl.tp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.HeatingService;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.ControlCallbacks;
import org.the.ems.ctrl.ControlledHeating;

public class TwoPointHeating extends ControlledHeating {
	private final static Logger logger = LoggerFactory.getLogger(TwoPointHeating.class);

	final double temperatureMaximum;
	final double temperatureMinimum;

	double temperatureValue;

	protected TwoPointHeating(ControlCallbacks callbacks, HeatingService component, 
			Channel temperature, double temperatureMaximum, double temperatureMinimum) {
		super(callbacks, component);
		this.temperatureMaximum = temperatureMaximum;
		this.temperatureMinimum = temperatureMinimum;
		temperature.registerValueListener(new TemperatureListener());
	}

	public double getMaximumTemperature() {
		return temperatureMaximum;
	}

	public boolean isBelowMaximumTemperature() {
		return isBelowMaximumTemperature(temperatureValue);
	}

	protected boolean isBelowMaximumTemperature(double temperature) {
		return temperature < temperatureMaximum;
	}

	public double getMinimumTemperature() {
		return temperatureMinimum;
	}

	public boolean isAboveMinimumTemperature() {
		return isAboveMinimumTemperature(temperatureValue);
	}

	protected boolean isAboveMinimumTemperature(double temperature) {
		return temperature > temperatureMinimum;
	}

	@Override
	public boolean isStartable() {
		if (!isAboveMinimumTemperature()) {
			return false;
		}
		return super.isStartable();
	}

	@Override
	public boolean isStoppable() {
		if (!isBelowMaximumTemperature()) {
			return false;
		}
		return super.isStoppable();
	}

	protected void onTemperatureChanged(Value value) {
		double temperature = value.doubleValue();
		if (!isAboveMinimumTemperature(temperature)) {
			doStart();
		}
		else if (!isBelowMaximumTemperature(temperature)) {
			doStop();
		}
	}

	class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			logger.debug("Received {} temperature value: {}°C", getId(), value);
			
	        if (temperatureValue != value.doubleValue()) {
	        	onTemperatureChanged(value);
	        }
	        temperatureValue = value.doubleValue();
		}
    }

}
