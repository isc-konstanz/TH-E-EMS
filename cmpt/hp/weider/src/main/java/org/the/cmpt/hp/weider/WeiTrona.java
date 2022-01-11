package org.the.cmpt.hp.weider;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.hp.HeatPump;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingType;
import org.the.ems.core.Season;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
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

	@SuppressWarnings("serial")
	private final Map<HeatingType, WeiderHeatingHandler> heatings = new HashMap<HeatingType, WeiderHeatingHandler>() {{
		put(HeatingType.HEATING_WATER, new WeiderHeatingHandler(HeatingType.HEATING_WATER));
		put(HeatingType.DOMESTIC_WATER, new WeiderHeatingHandler(HeatingType.DOMESTIC_WATER));
	}};

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
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		registerService(getId().concat("_").concat("hot_water"), configs, heatings.get(HeatingType.HEATING_WATER));
		registerService(getId().concat("_").concat("warm_water"), configs, heatings.get(HeatingType.DOMESTIC_WATER));
	}

	@Override
	protected void onStart(WriteContainer container, HeatingSettings settings) throws ComponentException {
		WeiderHeatingHandler heating = heatings.get(settings.getType());
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
		} catch (InvalidValueException e) {
			// Do nothing
		}
		if (settings.getValue().doubleValue() == getMaxPower()) {
			type = HeatingType.HEATING_WATER;
		}
		onStart(container, new HeatingSettings(type, time));
	}

	public boolean isStartable(HeatingType type) {
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
			// The heating water pump will always be shown as true, even if domestic water is beeing prepared
			boolean running = heatings.get(HeatingType.DOMESTIC_WATER).isRunning();
			switch (type) {
			case DOMESTIC_WATER:
				return running;
			case HEATING_WATER:
			default:
				return !running;
			}
		}
		return false;
	}

	@Override
	public boolean isRunning() throws ComponentException {
		return heatings.values().stream().anyMatch(c -> c.isRunning());
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		for (WeiderHeatingHandler heating : heatings.values()) {
			if (!heating.isStoppable() && !settings.isEnforced()) {
				logger.warn("Unable to stop component");
				continue;
			}
			heating.onStop(container, time);
		}
	}

	public boolean isStoppable(HeatingType type) {
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
		return heatings.values().stream().anyMatch(c -> c.isStandby());
	}

}
