package org.the.ems.cmpt.hp.weider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.hp.HeatPump;
import org.the.ems.cmpt.util.PowerListener;
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
import org.the.ems.core.data.DoubleValue;
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

	// TODO: Make this type specific configurable
	private static final double OPERATION_HIGH_TEMP = 50;
	private static final double OPERATION_HIGH_COP = 3;

	private static final double OPERATION_LOW_TEMP = 25;
	private static final double OPERATION_LOW_COP = 5;

	@Configuration
	private double cop;

	@Configuration
	private Channel season;

	@Configuration(mandatory = false)
	private Channel vacation;

	@Configuration(value=THERMAL_ENERGY_VALUE, mandatory=false)
	private Channel thermalEnergy;

	@Configuration(value=THERMAL_POWER_VALUE)
	private Channel thermalPower;

	private ThermalPowerListener thermalPowerListener;

	@Configuration(value=ELECTRICAL_ENERGY_VALUE, mandatory=false)
	private Channel electricalEnergy;

	@Configuration(value=ELECTRICAL_POWER_VALUE, mandatory=false)
	private Channel electricalPower;

	@Configuration(value="el_power_estimate", mandatory=false)
	private boolean electricalPowerEstimate = true;
	private ElectricalPowerListener electricalPowerListener;

	private final Map<HeatingType, HeatingHandler> heatings = new HashMap<HeatingType, HeatingHandler>();

	public boolean isVacation() throws ComponentException, InvalidValueException {
		if (vacation == null) {
			return false;
		}
		return vacation.getLatestValue().booleanValue();
	}

	@Override
	public Season getSeason() throws InvalidValueException {
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

	public boolean isSeason(Season season) throws InvalidValueException {
		return getSeason() == season;
	}

	private Stream<HeatingHandler> getHeatings() {
		return heatings.values().stream().filter(h -> h.isEnabled());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Value getElectricalPower() {
		return electricalPowerListener.getLatestValue();
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
		
		electricalPowerListener = new ElectricalPowerListener();
		electricalPower.registerValueListener(electricalPowerListener);
		
		thermalPowerListener = new ThermalPowerListener();
		thermalPower.registerValueListener(thermalPowerListener);
	}

	protected void onActivate(Configurations configs, HeatingType type) throws ComponentException {
		HeatingHandler handler = new HeatingHandler(this, type);
		getContext().registerService(getId().concat("_").concat(type.name().toLowerCase()), configs, handler);
		heatings.put(type, handler);
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		long time = settings.getEpochMillis();
		
		HeatingType type = HeatingType.DOMESTIC_WATER;
		try {
			if (isSeason(Season.SUMMER)) {
				type = HeatingType.HEATING_WATER;
			}
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving season mode: {}", e.getMessage());
			// Do nothing and continue
		}
		if (settings.getValue().doubleValue() == getMinPower()) {
			type = HeatingType.HEATING_WATER;
		}
		onStart(container, new HeatingSettings(type, time));
	}

	@Override
	protected void onStart(WriteContainer container, HeatingSettings settings) throws ComponentException {
		HeatingHandler heating = heatings.get(settings.getType());
		if (!heating.isStartable() && !settings.isEnforced()) {
			throw new ComponentException("Unable to start component");
		}
		try {
			if (isVacation()) {
				throw new ComponentException("Unable to start component during vacation");
			}
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving vacation mode: {}", e.getMessage());
			// Do nothing and continue
			// TODO: verify necessity or if start should be avoided in doubt
		}
		heating.onStart(container, settings.getEpochMillis());
	}

	public boolean isStartable(HeatingType type) {
		try {
			if (isSeason(Season.SUMMER) && type == HeatingType.HEATING_WATER) {
				return false;
			}
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving season mode: {}", e.getMessage());
			// Do nothing and continue
		}
		return heatings.get(type).isStartable();
	}

	@Override
	public boolean isStartable(long time) {
		if (!getHeatings().anyMatch(c -> c.isStartable())) {
			return false;
		}
		return super.isStartable(time);
	}

	@Override
	public boolean isRunning(HeatingType type) throws ComponentException, InvalidValueException {
		if (isRunning()) {
			try {
				if (isSeason(Season.SUMMER) && type == HeatingType.HEATING_WATER) {
					return false;
				}
				else {
					return true;
				}
			} catch (InvalidValueException e) {
				logger.debug("Error retrieving season mode: {}", e.getMessage());
				// Do nothing and continue
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
	public boolean isRunning() throws ComponentException, InvalidValueException {
		return state.getLatestValue().booleanValue();
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
			if (isSeason(Season.SUMMER) && type == HeatingType.HEATING_WATER) {
				return false;
			}
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving season mode: {}", e.getMessage());
			// Do nothing and continue
		}
		return heatings.get(type).isStoppable();
	}

	@Override
	public boolean isStoppable(long time) {
		if (!getHeatings().anyMatch(c -> c.isStoppable())) {
			return false;
		}
		return super.isStoppable(time);
	}

	@Override
	public boolean isStandby(HeatingType type) throws ComponentException, InvalidValueException {
		return !isRunning(type);
	}

	@Override
	public boolean isStandby() throws ComponentException, InvalidValueException {
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
			if (temperature.doubleValue() < temperatureSetpoint.doubleValue() - handler.getTemperatureHysteresis()) {
				setState(RunState.STARTING);
			}
			break;
		case STARTING:
		case RUNNING:
			if (temperature.doubleValue() >= temperatureSetpoint.doubleValue()) {
				setState(RunState.STOPPING);
			}
			break;
		default:
			break;
		}
	}

	private class ElectricalPowerListener extends PowerListener {

		private volatile Value powerValue = DoubleValue.emptyValue();

		private ElectricalPowerListener() {
			super(electricalEnergy);
		}

		public Value getLatestValue() {
			return powerValue;
		}

		public void setLatestValue(Value thermalPowerValue) throws ComponentException {
			try {
				double electricalPowerValue = 0;
				if (isRunning()) {
					double temperatureFlow = circulation.getFlowInletTemperature().doubleValue();
					double temperatureFactor = 
							(temperatureFlow - OPERATION_LOW_TEMP) / 
							(OPERATION_HIGH_TEMP - OPERATION_LOW_TEMP);
					
					double performanceFactor = OPERATION_LOW_COP + (OPERATION_HIGH_COP - OPERATION_LOW_COP)*temperatureFactor;
					double performancePowerValue = thermalPowerValue.doubleValue()/performanceFactor;
					double staticPowerValue = getMinPower() + (getMaxPower() - getMinPower())*temperatureFactor;
					
					electricalPowerValue = (performancePowerValue + staticPowerValue)/2;
				}
				electricalPower.setLatestValue(new DoubleValue(electricalPowerValue, thermalPowerValue.getEpochMillis()));
				
			} catch (InvalidValueException e) {
				// Do nothing
			}
		}

		@Override
		protected void onPowerReceived(Value powerValue) {
			super.onPowerReceived(powerValue);
			this.powerValue = powerValue;
		}
	}

	private class ThermalPowerListener extends PowerListener {

		private ThermalPowerListener() {
			super(thermalEnergy);
		}

		@Override
		protected void onPowerReceived(Value powerValue) {
			super.onPowerReceived(powerValue);
			try {
				electricalPowerListener.setLatestValue(powerValue);
				
			} catch (ComponentException e) {
				logger.warn("Error calculationg power estimate: {}", e.getMessage());
			}
		}
	}

}
