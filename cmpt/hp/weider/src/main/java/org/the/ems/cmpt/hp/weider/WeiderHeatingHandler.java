package org.the.ems.cmpt.hp.weider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingType;
import org.the.ems.core.RunState;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

public class WeiderHeatingHandler extends Component {
	private static final Logger logger = LoggerFactory.getLogger(WeiderHeatingHandler.class);

	private final WeiTrona heatPump;

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

	public WeiderHeatingHandler(WeiTrona heatPump, HeatingType type) {
		super(type.getFullName());
		this.type = type;
		this.heatPump = heatPump;
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

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		this.waterTempSetpoint.registerValueListener(new ValueListener() {

			@Override
			public void onValueChanged(Value waterTempSetpoint) {
				try {
					double waterTempValue = waterTemp.getLatestValue().doubleValue();
					
					switch (heatPump.getState()) {
					case STANDBY:
					case STOPPING:
						if (waterTempValue < waterTempSetpoint.doubleValue() - getHysteresisTemperature()) {
							heatPump.setState(RunState.STARTING);
						}
						break;
					case STARTING:
					case RUNNING:
						if (waterTempValue >= waterTempSetpoint.doubleValue() + getHysteresisTemperature()) {
							heatPump.setState(RunState.STOPPING);
						}
						break;
					default:
						break;
					}
				} catch (InvalidValueException e) {
					logger.debug("Error retrieving {} temperature: {}", type.toString().toLowerCase(),  
							e.getMessage());
				}
			}
		});
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		this.waterTempSetpoint.deregisterValueListeners();
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
			return heatPump.getStateValue().booleanValue() && 
					pumpState.getLatestValue().booleanValue();
			
		} catch (InvalidValueException | ComponentException e) {
			logger.debug("Error retrieving {} state: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		return false;
	}

	public void onStop(WriteContainer container, long time) {
		container.addDouble(waterTempSetpoint, getStopSetpoint(), time);
	}

	private double getStopSetpoint() {
		return waterTempMin + getHysteresisTemperature();
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

	private double getHysteresisTemperature() {
		try {
			return waterTempHysteresis.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature hysteresis: {}", type.toString().toLowerCase(),  
					e.getMessage());
			
			return waterTempHysteresisFallback;
		}
	}

}
