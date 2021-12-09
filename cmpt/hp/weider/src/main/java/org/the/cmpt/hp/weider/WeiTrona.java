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
import org.the.ems.core.HeatingMode;
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
	private final Map<HeatingMode, HeatingHandler> handlers = new HashMap<HeatingMode, HeatingHandler>() {{
		put(HeatingMode.HEATING_WATER, new HeatingHandler(HeatingMode.HEATING_WATER));
		put(HeatingMode.DOMESTIC_WATER, new HeatingHandler(HeatingMode.DOMESTIC_WATER));
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
		registerService(getId().concat("_").concat("hot_water"), configs, handlers.get(HeatingMode.HEATING_WATER));
		registerService(getId().concat("_").concat("warm_water"), configs, handlers.get(HeatingMode.DOMESTIC_WATER));
	}

	@Override
	protected void onStart(WriteContainer container, HeatingSettings settings) throws ComponentException {
		HeatingHandler handler = handlers.get(settings.getMode());
		if (!handler.isStartable() && !settings.isEnforced()) {
			throw new ComponentException("Unable to start component");
		}
		handler.onStart(container, settings.getEpochMillis());
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		
		HeatingMode mode = HeatingMode.DOMESTIC_WATER;
		try {
			if (getSeason() == Season.SUMMER) {
				mode = HeatingMode.HEATING_WATER;
			}
		} catch (InvalidValueException e) {
			// Do nothing
		}
		if (settings.getValue().doubleValue() == getMaxPower()) {
			mode = HeatingMode.HEATING_WATER;
		}
		onStart(container, new HeatingSettings(mode, time));
	}

	@Override
	public boolean isStartable(long time) {
		if (!handlers.values().stream().anyMatch(c -> c.isStartable())) {
			return false;
		}
		return super.isStartable(time);
	}

	@Override
	public boolean isRunning() throws ComponentException {
		return handlers.values().stream().anyMatch(c -> c.isRunning());
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		for (HeatingHandler handler : handlers.values()) {
			if (!handler.isStoppable() && !settings.isEnforced()) {
				logger.warn("Unable to stop component");
				continue;
			}
			handler.onStop(container, time);
		}
	}

	@Override
	public boolean isStoppable(long time) {
		if (!handlers.values().stream().anyMatch(c -> c.isStoppable())) {
			return false;
		}
		return super.isStoppable(time);
	}

	@Override
	public boolean isStandby() throws ComponentException {
		return handlers.values().stream().anyMatch(c -> c.isStandby());
	}

}
