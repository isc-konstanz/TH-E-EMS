package org.the.ems.cmpt.tes;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.HeatStorage;
import org.the.ems.core.ComponentContext;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.HeatingType;
import org.the.ems.core.cmpt.StratifiedChargeStorage;
import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationCollection.DoubleCollection;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.schedule.NamedThreadFactory;

@org.osgi.service.component.annotations.Component(
	scope = ServiceScope.BUNDLE,
	service = ThermalEnergyStorageService.class,
	configurationPid = ThermalEnergyStorageService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ThermalEnergyStorage extends HeatStorage 
		implements StratifiedChargeStorage, ValueListener, Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ThermalEnergyStorage.class);

	private static final int SECONDS_IN_HOUR = 3600;
	private static final int MILLIS_IN_HOUR = 3600000;

	protected static final String TEMP_WATER_DOM_VALUE = "temp_dom";
	protected static final String TEMP_WATER_HT_VALUE = "temp_ht";

	protected ScheduledExecutorService executor;

	@Configuration(mandatory=false, scale=60000) // Default interval of 15 minutes
	protected int interval = 900000;

	// The specific heat capacity of the storage medium. Default is 4.1813 of water.
	@Configuration(mandatory=false)
	protected double specificHeat = 4.1813;

	// The density of the storage medium. Default is 1 of water.
	@Configuration(mandatory=false)
	protected double density = 1;

	@Configuration
	protected double capacity;

	protected double mass;

	@Configuration(value="weight_*")
	protected DoubleCollection weights;

	@Configuration(value="temp_*")
	protected ChannelCollection temperatures;

	@Configuration(value="temp")
	protected Channel temperature;

	protected Value temperatureLast = null;
	protected long timestampLast = -1;

	@Configuration
	protected Channel power;

	@Configuration
	protected Channel energy;
	protected double energyLast = 0;

	protected final Map<String, ThermalEnergy> heatingEnergies = new HashMap<String, ThermalEnergy>();

	@Override
	protected void onBindHeating(HeatingService heatingService) {
		ThermalEnergy energy = new ThermalEnergy(heatingService);
		heatingEnergies.put(heatingService.getId(), energy);
	}

	@Override
	protected void onUnbindHeating(HeatingService heatingService) {
		heatingEnergies.remove(heatingService.getId());
	}

	@Override
	protected void onActivate(ComponentContext context, Configurations configs) throws ComponentException {
		super.onActivate(context, configs);
		for (Channel temperature : temperatures.values()) {
			temperature.registerValueListener(this);
		}
		
		// Storage medium mass in kilogram
		mass = capacity*density;
		
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E EMS "+getId().toUpperCase()+" Pool - thread-");
		executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);
		
		LocalTime time = LocalTime.now();
		LocalTime next = time.with(next(interval));
		logger.debug("Starting TH-E EMS {} power calculation {}", getId().toUpperCase(), next);
		
		executor.scheduleAtFixedRate(this, 
				time.until(next, ChronoUnit.MILLIS), interval, TimeUnit.MILLISECONDS);
			
	}

	@Override
	protected void onConfigure(Configurations configs) throws ConfigurationException {
		super.onConfigure(configs);
		for (String temperatureKey : temperatures.keySet()) {
			String weightKey = temperatureKey.replace("temp_", "weight_");
			if (!weights.contains(weightKey)) {
				weights.add(weightKey, (double) temperatures.size());
			}
		}
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		executor.shutdown();
	}

	@Override
	public void run() {
		try {
			long time = System.currentTimeMillis();
			double temperature;
			if (temperatures.size() > 0) {
				temperature = getWeightedWaterTemperature();
			}
			else {
				Value temp = getTemperature();
				if (temp == null) {
					return;
				}
				temperature = temp.doubleValue();
			}
			double energyInput = 0;
			synchronized (heatingEnergies) {
				for (ThermalEnergy energy : heatingEnergies.values()) {
					energyInput += energy.getValue().doubleValue();
				}
			}
			if (temperatureLast != null) {
				double hoursDelta = ((double) time - temperatureLast.getEpochMillis())/MILLIS_IN_HOUR;
				double tempDelta = temperature - temperatureLast.doubleValue();
				
				// Calculate energy in Q[kJ] = cp*m[kg]*dT[°C]
				double jouleDelta = specificHeat*mass*tempDelta;
				
				double energyValue;
				double energyDelta = jouleDelta/SECONDS_IN_HOUR;
				
				// When the tank temperature is dropping, the input energy is insufficient.
				// The real energy consumption is the sum of measured and input energy
				if (energyDelta < 0) {
					energyValue = energyInput + Math.abs(energyDelta);
				}
				// When the tank temperature is rising, energy consumption is what's left of 
				// the input energy
				else if (energyDelta > 0) {
					energyValue = energyInput - energyDelta;
				}
				else {
					energyValue = energyInput;
				}
				energyValue = Math.max(energyValue, 0);
				
				energyLast += energyValue;
				energy.setLatestValue(new DoubleValue(energyLast, time));
				power.setLatestValue(new DoubleValue(energyValue*1000/hoursDelta, time));
			}
			temperatureLast = new DoubleValue(temperature, time);
			
		} catch (Exception e) {
			logger.warn("Error calculating power: {}", e.getMessage());
		}
	}

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	public Value getThermalPower() throws ComponentException, InvalidValueException {
		return power.getLatestValue();
	}

	@Override
	public Value getThermalPower(ValueListener listener) throws ComponentException, InvalidValueException {
		return power.getLatestValue(listener);
	}

	@Override
	public void registerThermalPowerListener(ValueListener listener) throws ComponentException {
		power.registerValueListener(listener);
	}

	@Override
	public void deregisterThermalPowerListener(ValueListener listener) throws ComponentException {
		power.deregisterValueListener(listener);
	}

	@Override
	public Value getTemperature() throws ComponentException, InvalidValueException {
		return temperature.getLatestValue();
	}

	@Override
	public Value getTemperature(ValueListener listener) throws ComponentException, InvalidValueException {
		return temperature.getLatestValue(listener);
	}

	@Override
	public void registerTemperaturereListener(ValueListener listener) throws ComponentException {
		temperature.registerValueListener(listener);
	}

	@Override
	public void deregisterTemperatureListener(ValueListener listener) throws ComponentException {
		temperature.deregisterValueListener(listener);
	}

	@Configuration(value=TEMP_WATER_DOM_VALUE, mandatory=false)
	protected Channel getDomesticWaterTemperature() throws ComponentException {
		return getContext().getDefaultChannel(TEMP_WATER_DOM_VALUE);
	}

	@Configuration(value=TEMP_WATER_HT_VALUE, mandatory=false)
	protected Channel getHeatingWaterTemperature() throws ComponentException {
		return getContext().getDefaultChannel(TEMP_WATER_HT_VALUE);
	}

	@Override
	public Value getTemperature(HeatingType type) throws ComponentException, InvalidValueException {
		switch (type) {
		case DOMESTIC_WATER:
			return getDomesticWaterTemperature().getLatestValue();
		case HEATING_WATER:
			return getHeatingWaterTemperature().getLatestValue();
		default:
			throw new IllegalArgumentException("Unknown heating type: " + type);
		}
	}

	@Override
	public Value getTemperature(HeatingType type, ValueListener listener) throws ComponentException, InvalidValueException {
		switch (type) {
		case DOMESTIC_WATER:
			return getDomesticWaterTemperature().getLatestValue(listener);
		case HEATING_WATER:
			return getHeatingWaterTemperature().getLatestValue(listener);
		default:
			throw new IllegalArgumentException("Unknown heating type: " + type);
		}
	}

	@Override
	public void registerTemperaturereListener(HeatingType type, ValueListener listener) throws ComponentException {
		switch (type) {
		case DOMESTIC_WATER:
			getDomesticWaterTemperature().registerValueListener(listener);
		case HEATING_WATER:
			getHeatingWaterTemperature().registerValueListener(listener);
		default:
			throw new IllegalArgumentException("Unknown heating type: " + type);
		}
	}

	@Override
	public void deregisterTemperatureListener(HeatingType type, ValueListener listener) throws ComponentException {
		switch (type) {
		case DOMESTIC_WATER:
			getDomesticWaterTemperature().deregisterValueListener(listener);
		case HEATING_WATER:
			getHeatingWaterTemperature().deregisterValueListener(listener);
		default:
			throw new IllegalArgumentException("Unknown heating type: " + type);
		}
	}

	protected double getWeightedWaterTemperature() throws InvalidValueException {
		double temperature = 0;
		for (String temperatureKey : temperatures.keySet()) {
			String weightKey = temperatureKey.replace("temp_", "weight_");
			temperature += (temperatures.get(temperatureKey).getLatestValue().doubleValue()
					/ weights.get(weightKey));
		}
		return temperature;
	}

	@Override
	public void onValueReceived(Value value) {
		long timestampMax = -1;
		try {
			for (Channel channel : temperatures.values()) {
				Value temperatureValue = channel.getLatestValue();
				if (temperatureValue.getEpochMillis() <= timestampLast) {
					return;
				}
				if (temperatureValue.getEpochMillis() > timestampMax) {
					timestampMax = temperatureValue.getEpochMillis();
				}
			}
			temperature.setLatestValue(new DoubleValue(getWeightedWaterTemperature(), timestampMax));
			timestampLast = timestampMax;
			
		} catch (InvalidValueException e) {
			logger.debug("Unable to calculate weighted storage temperature: {}", e.getMessage());
		}
	}

	private TemporalAdjuster next(int interval) {
		int minutes = interval/60000;
		return (temporal) -> {
			int minute = temporal.get(ChronoField.MINUTE_OF_DAY);
			int next = (minute / minutes + 1) * minutes;
			return temporal.with(ChronoField.SECOND_OF_DAY, 0)
					.with(ChronoField.MILLI_OF_DAY, 0)
					.with(ChronoField.NANO_OF_DAY, 0)
					.plus(next, ChronoUnit.MINUTES);
		};
	}

}
