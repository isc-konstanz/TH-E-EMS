package org.the.cmpt.hp.weider;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.cmpt.hp.HeatPump;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.RunState;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = HeatPumpService.class,
	configurationPid = HeatPumpService.PID+".weider",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class WeiTrona extends HeatPump {

	@Configuration
	private double cop;

	@Configuration
	private Channel season;

	@SuppressWarnings("serial")
	private final Map<ChargingMode, ChargingHandler> chargingHandlers = new HashMap<ChargingMode, ChargingHandler>() {{
		put(ChargingMode.HOT_WATER, new ChargingHandler(ChargingMode.HOT_WATER));
		put(ChargingMode.WARM_WATER, new ChargingHandler(ChargingMode.WARM_WATER));
	}};

	public Season getSeason() throws InvalidValueException {
		return Season.valueOf(season.getLatestValue().intValue());
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		registerService(getId().concat("_").concat("hot_water"), configs, chargingHandlers.get(ChargingMode.HOT_WATER));
		registerService(getId().concat("_").concat("warm_water"), configs, chargingHandlers.get(ChargingMode.WARM_WATER));
	}

	public void start(ChargingMode mode) throws EnergyManagementException {
		long timestamp = System.currentTimeMillis();
		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (!isStartable(timestamp, mode)) {
			throw new ComponentException(MessageFormat.format("Unable to start {0} heating", 
					mode.toString().toLowerCase()));
		}
		doStart(mode, timestamp);
	}

	void doStart(ChargingMode mode, long timestamp) throws EnergyManagementException {
		WriteContainer writeContainer = new WriteContainer();
		setState(RunState.STARTING);
		onStart(writeContainer, mode, timestamp);
		write(writeContainer);
		startTimeLast = timestamp;
	}

	protected void onStart(WriteContainer container, ChargingMode mode, long timestamp) throws ComponentException {
		chargingHandlers.get(mode).onStart(container, timestamp);
	}

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		ChargingMode mode = ChargingMode.WARM_WATER;
		try {
			if (getSeason() == Season.SUMMER) {
				mode = ChargingMode.HOT_WATER;
			}
		} catch (InvalidValueException e) {
			// Do nothing
		}
		if (value.doubleValue() == getMaxPower()) {
			mode = ChargingMode.HOT_WATER;
		}
		onStart(container, mode, value.getTime());
	}

	public boolean isStartable(long time, ChargingMode mode) {
		if (!chargingHandlers.get(mode).isStartable()) {
			return false;
		}
		return super.isStartable(time);
	}

	@Override
	public boolean isStartable(long time) {
		if (!chargingHandlers.values().stream().anyMatch(c -> c.isStartable())) {
			return false;
		}
		return super.isStartable(time);
	}

	@Override
	public boolean isRunning() throws ComponentException {
		return chargingHandlers.values().stream().anyMatch(c -> c.isRunning());
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		chargingHandlers.values().stream().forEach(c -> 
				c.onStop(container, time)
		);
	}

	public boolean isStoppable(long time, ChargingMode mode) {
		if (!chargingHandlers.get(mode).isStoppable()) {
			return false;
		}
		return super.isStoppable(time);
	}

	@Override
	public boolean isStoppable(long time) {
		if (!chargingHandlers.values().stream().anyMatch(c -> c.isStoppable())) {
			return false;
		}
		return super.isStoppable(time);
	}

	@Override
	public boolean isStandby() throws ComponentException {
		return chargingHandlers.values().stream().anyMatch(c -> c.isStandby());
	}

}
