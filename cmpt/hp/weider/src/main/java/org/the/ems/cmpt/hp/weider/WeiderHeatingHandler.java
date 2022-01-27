package org.the.ems.cmpt.hp.weider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.HeatingType;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.WriteContainer;

public class WeiderHeatingHandler extends Component {
	private static final Logger logger = LoggerFactory.getLogger(WeiderHeatingHandler.class);

	@Configuration(value = "state", section = Configurations.GENERAL)
	private Channel compressorState;

	@Configuration(value = "state_pump")
	private Channel pumpState;


	@Configuration
	private double waterTempMax;

	@Configuration
	private double waterTempMin;


	@Configuration
	private Channel waterTemp;

	@Configuration
	private Channel waterTempSetpoint;

	@Configuration
	private Channel waterTempHysteresis;

	@Configuration(mandatory = false)
	private Double waterTempHysteresisFallback = Double.NaN;


	final HeatingType type;

	public WeiderHeatingHandler(HeatingType type) {
		super(type.getFullName());
		this.type = type;
	}

	@Override
	protected void onConfigure(Configurations configs) throws ConfigurationException {
		super.onConfigure(configs);

		if (waterTempHysteresisFallback.isNaN()) {
			switch (type) {
			case DOMESTIC_WATER:
				waterTempHysteresisFallback = 4.0;
				break;
			case HEATING_WATER:
			default:
				waterTempHysteresisFallback = 2.0;
				break;
			}
			logger.debug("Configured hysteresis fallback value for {} handler: {}", 
					type.getFullName(), waterTempHysteresisFallback);
		}
	}

	public void onStart(WriteContainer container, long time) {
		container.addDouble(waterTempSetpoint, getStartSetpoint(), time);
	}

	private double getStartSetpoint() {
		return waterTempMax;
	}

	public boolean isStarted() {
		try {
			return waterTempSetpoint.getLatestValue().doubleValue() >= getStartSetpoint();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature setpoint: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		return false;
	}

	public boolean isStartable() {
		try {
			return waterTemp.getLatestValue().doubleValue() < waterTempMax;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		return true;
	}

	public boolean isRunning() {
		try {
			return compressorState.getLatestValue().booleanValue() && 
					pumpState.getLatestValue().booleanValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} state: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		return false;
	}

	public void onStop(WriteContainer container, long time) {
		container.addDouble(waterTempSetpoint, getStopSetpoint(), time);
	}

	private double getStopSetpoint() {
		double waterTempHysteresisValue;
		try {
			waterTempHysteresisValue= waterTempHysteresis.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature hysteresis: {}", type.toString().toLowerCase(),  
					e.getMessage());
			
			waterTempHysteresisValue = waterTempHysteresisFallback;
		}
		return waterTempMin + waterTempHysteresisValue;
	}

	public boolean isStopped() {
		try {
			return waterTempSetpoint.getLatestValue().doubleValue() <= getStopSetpoint();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature setpoint: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		return false;
	}

	public boolean isStoppable() {
		try {
			return waterTemp.getLatestValue().doubleValue() > waterTempMin;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		return true;
	}

	public boolean isStandby() {
		return !isRunning();
	}

}
