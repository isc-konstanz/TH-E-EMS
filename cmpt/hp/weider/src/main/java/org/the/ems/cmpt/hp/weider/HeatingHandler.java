package org.the.ems.cmpt.hp.weider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingType;
import org.the.ems.core.Season;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

public class HeatingHandler extends Component implements ValueListener {
	private static final Logger logger = LoggerFactory.getLogger(HeatingHandler.class);

	private static final String WATER_TEMP_MAX = "water_temp_max";
	private static final String WATER_TEMP_MAX_DEFAULT = "water_temp_min_default";
	private static final String WATER_TEMP_MAX_FALLBACK = "water_temp_min_fallback";

	private static final String WATER_TEMP_MIN = "water_temp_min";
	private static final String WATER_TEMP_MIN_DEFAULT = "water_temp_min_default";
	private static final String WATER_TEMP_MIN_FALLBACK = "water_temp_min_fallback";

	private final WeiTrona heatPump;

	@Configuration(value = "state_pump")
	private Channel pumpState;

	private Double waterTempMax = Double.NaN;

	@Configuration(value= {WATER_TEMP_MAX_DEFAULT, WATER_TEMP_MAX_FALLBACK}, mandatory = false)
	private Double waterTempMaxDefault = Double.NaN;

	private Double waterTempMin = Double.NaN;

	@Configuration(value= {WATER_TEMP_MIN_DEFAULT, WATER_TEMP_MIN_FALLBACK}, mandatory = false)
	private Double waterTempMinDefault = Double.NaN;

	@Configuration
	private Channel waterTemp;

	@Configuration
	private Channel waterTempHysteresis;

	@Configuration(mandatory = false)
	private Double waterTempHysteresisFallback = Double.NaN;

	@Configuration
	private Channel waterTempSetpoint;

	private double waterTempSetpointValue;


	final HeatingType type;

	public HeatingHandler(WeiTrona heatPump, HeatingType type) {
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
				waterTempHysteresisFallback = 4.;
				break;
			case HEATING_WATER:
			default:
				waterTempHysteresisFallback = 2.;
				break;
			}
			logger.debug("Configured hysteresis fallback value for {} handler: {}", 
					type.getFullName(), waterTempHysteresisFallback);
		}
		if (waterTempMaxDefault.isNaN()) {
			waterTempMaxDefault = 55.;
			logger.debug("Configured maximum fallback value for {} handler: {}", 
					type.getFullName(), waterTempMaxDefault);
		}
		this.waterTempMax = getTemperatureValue(WATER_TEMP_MAX);
		if (waterTempMax.isNaN()) {
			try {
				Channel waterTempMaxChannel = getContext().getDefaultChannel(WATER_TEMP_MAX);
				this.waterTempMax = waterTempMaxChannel.getLatestValue(new ValueListener() {
					@Override
					public void onValueChanged(Value temperature) {
						waterTempMax = temperature.doubleValue();
					}
				}).doubleValue();
			} catch (InvalidValueException e) {
				waterTempMax = waterTempMaxDefault;
			}
		}
		if (waterTempMinDefault.isNaN()) {
			switch (type) {
			case DOMESTIC_WATER:
				waterTempMinDefault = 41.;
				break;
			case HEATING_WATER:
			default:
				waterTempMinDefault = 21.;
				break;
			}
			logger.debug("Configured minimum fallback value for {} handler: {}", 
					type.getFullName(), waterTempMinDefault);
		}
		this.waterTempMin = getTemperatureValue(WATER_TEMP_MIN);
		if (!waterTempMin.isNaN()) {
			waterTempSetpointValue = waterTempMin + getTemperatureHysteresis();
		}
		else {
			waterTempSetpointValue = waterTempMinDefault + getTemperatureHysteresis();
			try {
				Channel waterTempMinChannel = getContext().getDefaultChannel(WATER_TEMP_MIN);
				this.waterTempMin = waterTempMinChannel.getLatestValue(new ValueListener() {
					@Override
					public void onValueChanged(Value temperature) {
						waterTempMin = temperature.doubleValue() - getTemperatureHysteresis();
					}
				}).doubleValue();
			} catch (InvalidValueException e) {
				waterTempMin = waterTempMinDefault;
			}
		}
		logger.info("Starting WeiTrona {} handler from {} to {}°C", type.getFullName(), 
				String.format("%.1f", getTemperatureMinimum()),
				String.format("%.1f", getTemperatureMaximum()));
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		this.waterTemp.registerValueListener(this);
		this.waterTempSetpoint.registerValueListener(this);
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		this.waterTemp.deregisterValueListeners();
		this.waterTempSetpoint.deregisterValueListeners();
	}

	public void onStart(WriteContainer container, long time) {
		double temperatureSetpoint = getStartSetpoint();
		try {
			container.addDoubleIfChanged(waterTempSetpoint, temperatureSetpoint);
			return;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature setpoint: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		container.addDouble(waterTempSetpoint, temperatureSetpoint, time);
	}

	private double getStartSetpoint() {
		return getTemperatureMaximum();
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
		if (!isEnabled()) {
			return false;
		}
		try {
			return waterTemp.getLatestValue().doubleValue() < getTemperatureMaximum() - getTemperatureHysteresis();
			
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
		double temperatureSetpoint = getStopSetpoint();
		try {
			container.addDoubleIfChanged(waterTempSetpoint, temperatureSetpoint);
			return;
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature setpoint: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		container.addDouble(waterTempSetpoint, temperatureSetpoint, time);
	}

	private double getStopSetpoint() {
		return getTemperatureSetpoint();
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
		if (!isEnabled()) {
			return false;
		}
		try {
			return waterTemp.getLatestValue().doubleValue() > getTemperatureMinimum();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
		return true;
	}

	public boolean isStandby() {
		return !isRunning();
	}

	Value getTemperature() throws InvalidValueException {
		return waterTemp.getLatestValue();
	}

	double getTemperatureSetpoint() {
		return waterTempSetpointValue;
	}

	double getTemperatureMaximum() {
		return waterTempMax;
	}

	double getTemperatureMinimum() {
		return waterTempMin;
	}

	double getTemperatureHysteresis() {
		try {
			return waterTempHysteresis.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature hysteresis: {}", type.toString().toLowerCase(),  
					e.getMessage());
			
			return waterTempHysteresisFallback;
		}
	}

	private double getTemperatureValue(String key) throws ConfigurationException {
		try {
			if (getConfigurations().containsKey(getDefaultSection(), key)) {
				return getConfigurations().getDouble(getDefaultSection(), key);
			}
		} catch (NumberFormatException e) {
			// Do nothing and return NaN
		}
		return Double.NaN;
	}

	@Override
	public void onValueChanged(Value value) {
		try {
			if (heatPump.isSeason(Season.WINTER) || type == HeatingType.DOMESTIC_WATER) {
				heatPump.onTemperatureChanged(this, waterTemp.getLatestValue(), waterTempSetpoint.getLatestValue());
			}
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving {} temperature setpoint: {}", type.toString().toLowerCase(),  
					e.getMessage());
		}
	}

}
