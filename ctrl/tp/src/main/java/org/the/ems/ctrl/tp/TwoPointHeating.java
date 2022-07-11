package org.the.ems.ctrl.tp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.HeatingControl;

public class TwoPointHeating extends HeatingControl {
	private final static Logger logger = LoggerFactory.getLogger(TwoPointHeating.class);

	final double temperatureMaximum;
	final double temperatureMinimum;

	double temperatureValue;

	protected TwoPointHeating(HeatingCallbacks callbacks, HeatingService component, 
			Channel temperature, double temperatureMaximum, double temperatureMinimum) throws ComponentException {
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
		if (!isBelowMaximumTemperature()) {
			return false;
		}
		if (!isAboveMinimumTemperature()) {
			return true;
		}
		return super.isStartable();
	}

	@Override
	public boolean isStoppable() {
		if (!isAboveMinimumTemperature()) {
			return false;
		}
		if (!isBelowMaximumTemperature()) {
			return true;
		}
		return super.isStoppable();
	}

	protected void onTemperatureChanged(Value value) {
		double temperature = value.doubleValue();
		if (!isAboveMinimumTemperature(temperature)) {
			start();
		}
		else if (!isBelowMaximumTemperature(temperature)) {
			stop();
		}
	}

	class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			logger.trace("Received {} temperature value: {}°C", getId(), value);
			
	        if (temperatureValue != value.doubleValue()) {
	        	onTemperatureChanged(value);
	        }
	        temperatureValue = value.doubleValue();
		}
    }

}
