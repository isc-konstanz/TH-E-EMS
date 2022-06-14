package org.the.ems.cmpt.hp.weider;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.hp.HeatPump;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.HeatingType;
import org.the.ems.core.RunState;
import org.the.ems.core.Season;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.settings.HeatingSettings;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.core.settings.ValueSettings;

@Component(
	scope = ServiceScope.BUNDLE,
	service = HeatPumpService.class,
	configurationPid = HeatPumpService.PID+".weider",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class WeiTrona extends HeatPump {
	private static final Logger logger = LoggerFactory.getLogger(WeiTrona.class);

	@Configuration
	private double cop;

	@Configuration
	private Channel season;

	private final Map<HeatingType, HeatingHandler> heatings = new HashMap<HeatingType, HeatingHandler>();

	@Override
	public Season getSeason() throws ComponentException, InvalidValueException {
		int code = season.getLatestValue().intValue();
		switch (code) {
		case 0:
			return Season.SUMMER;
		case 1:
			return Season.WINTER;
		default:
			break;
		}
        throw new IllegalArgumentException("Unknown Season code: " + code);
	}

	@Override
	protected void onConfigure(Configurations configs) throws ConfigurationException {
		// WeiTrone compressor state is not writable
		this.stateIsWritable = false;
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		this.onActivate(configs, HeatingType.HEATING_WATER);
		this.onActivate(configs, HeatingType.DOMESTIC_WATER);
	}

	protected void onActivate(Configurations configs, HeatingType type) throws ComponentException {
		HeatingHandler handler = new HeatingHandler(this, type);
		registerService(getId().concat("_").concat(type.name().toLowerCase()), configs, handler);
		heatings.put(type, handler);
	}

	@Override
	protected void onStart(WriteContainer container, HeatingSettings settings) throws ComponentException {
		HeatingHandler heating = heatings.get(settings.getType());
		if (!heating.isStartable() && !settings.isEnforced()) {
			throw new ComponentException("Unable to start component");
		}
		heating.onStart(container, settings.getEpochMillis());
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		
		HeatingType type = HeatingType.DOMESTIC_WATER;
		try {
			if (getSeason() == Season.SUMMER) {
				type = HeatingType.HEATING_WATER;
			}
		} catch (InvalidValueException | ComponentException e) {
			// Do nothing
		}
		if (settings.getValue().doubleValue() == getMaxPower()) {
			type = HeatingType.HEATING_WATER;
		}
		onStart(container, new HeatingSettings(type, time));
	}

	public boolean isStartable(HeatingType type) {
		try {
			if (type == HeatingType.HEATING_WATER && 
					getSeason() == Season.SUMMER) {
				return false;
			}
		} catch (InvalidValueException | ComponentException e) {
			// Do nothing
		}
		return heatings.get(type).isStartable();
	}

	@Override
	public boolean isStartable(long time) {
		if (!heatings.values().stream().anyMatch(c -> c.isStartable())) {
			return false;
		}
		return super.isStartable(time);
	}

	@Override
	public boolean isRunning(HeatingType type) throws ComponentException {
		if (isRunning()) {
			try {
				if (type == HeatingType.HEATING_WATER && 
						getSeason() == Season.SUMMER) {
					return false;
				}
				else {
					return true;
				}
			} catch (InvalidValueException | ComponentException e) {
				// Do nothing
			}
			// The heating water pump will always be shown as true, even if domestic water is beeing prepared
            boolean domesticWater = heatings.get(HeatingType.DOMESTIC_WATER).isRunning();
            switch (type) {
            case DOMESTIC_WATER:
                return domesticWater;
            case HEATING_WATER:
            default:
                return !domesticWater;
            }
		}
		return false;
	}

	@Override
	public boolean isRunning() throws ComponentException {
		try {
			return state.getLatestValue().booleanValue();
			
		} catch (InvalidValueException e) {
			throw new ComponentException(e);
		}
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		for (HeatingHandler heating : heatings.values()) {
			if (!heating.isStoppable() && !settings.isEnforced()) {
				logger.warn("Unable to stop component");
				continue;
			}
			heating.onStop(container, time);
		}
	}

	public boolean isStoppable(HeatingType type) {
		try {
			if (type == HeatingType.HEATING_WATER && 
					getSeason() == Season.SUMMER) {
				return false;
			}
		} catch (InvalidValueException | ComponentException e) {
			// Do nothing
		}
		return heatings.get(type).isStoppable();
	}

	@Override
	public boolean isStoppable(long time) {
		if (!heatings.values().stream().anyMatch(c -> c.isStoppable())) {
			return false;
		}
		return super.isStoppable(time);
	}

	@Override
	public boolean isStandby(HeatingType type) throws ComponentException {
		return !isRunning(type);
	}

	@Override
	public boolean isStandby() throws ComponentException {
		return !isRunning();
	}

	@Override
	protected void onStandby() throws ComponentException {
		super.onStandby();

		long timestamp = System.currentTimeMillis();
		for (HeatingHandler heating : heatings.values()) {
			try {
    			if (!heating.isStopped()) {
    				HeatingSettings heatingSettings = new HeatingSettings(heating.type, timestamp);
    				heatingSettings.setEnforced(true);
    				stop(heatingSettings);
    			}
			} catch (EnergyManagementException e) {
				logger.warn("Error verifying heating {} temperature setpoint value: {}",
						heating.type.toString().toLowerCase(), e.getMessage());
			}
		}
	}

	protected void onTemperatureChanged(HeatingHandler handler, Value temperature, Value temperatureSetpoint) {
		switch (getState()) {
		case STOPPING:
		case STANDBY:
			if (temperature.doubleValue() < temperatureSetpoint.doubleValue() - handler.getTemperatureHysteresis() &&
					isStartable(handler.type)) {
				setState(RunState.STARTING);
			}
			break;
		case STARTING:
		case RUNNING:
			if (temperature.doubleValue() >= temperatureSetpoint.doubleValue() &&
					isStoppable(handler.type)) {
				setState(RunState.STOPPING);
			}
			break;
		default:
			break;
		}
	}

}
