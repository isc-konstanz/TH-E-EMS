package org.the.cmpt.hp.weider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.HeatingMode;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.WriteContainer;

public class HeatingHandler extends Component {
	private static final Logger logger = LoggerFactory.getLogger(HeatingHandler.class);

	@Configuration(value = "state", section = Configurations.GENERAL)
	private Channel compressorState;

	@Configuration(value = "state_pump")
	private Channel pumpState;


	@Configuration
	private Channel waterTemp;

	@Configuration
	private Channel waterTempSetpoint;

	@Configuration
	private Channel waterTempHysteresis;

	@Configuration
	private double waterTempMax;

	@Configuration
	private double waterTempMin;


	private final HeatingMode mode;

	public HeatingHandler(HeatingMode mode) {
		super(mode.toString());
		this.mode = mode;
	}

	public void onStart(WriteContainer container, long time) {
		container.addDouble(waterTempSetpoint, waterTempMax, time);
	}

	public boolean isStartable() {
		try {
			return waterTemp.getLatestValue().doubleValue() <= waterTempMax;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature: {}", mode.toString().toLowerCase(),  
					e.getMessage());
		}
		return true;
	}

	public boolean isRunning() {
		try {
			return compressorState.getLatestValue().booleanValue() && 
					pumpState.getLatestValue().booleanValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} state: {}", mode.toString().toLowerCase(),  
					e.getMessage());
		}
		return false;
	}

	public void onStop(WriteContainer container, long time) {
		double waterTempSetpointValue = waterTempMin;
		try {
			waterTempSetpointValue += waterTempHysteresis.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature hysteresis: {}", mode.toString().toLowerCase(),  
					e.getMessage());
		}
		container.addDouble(waterTempSetpoint, waterTempSetpointValue, time);
	}

	public boolean isStoppable() {
		try {
			return waterTemp.getLatestValue().doubleValue() >= waterTempMin;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature: {}", mode.toString().toLowerCase(),  
					e.getMessage());
		}
		return true;
	}

	public boolean isStandby() {
		return !isRunning();
	}

}
