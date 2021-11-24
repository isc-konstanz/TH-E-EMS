package org.the.ems.cmpt.tes;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.EnergyManagementService;
import org.the.ems.core.HeatingService;
import org.the.ems.core.UnknownComponentException;
import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.config.Configuration;
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
public class ThermalEnergyStorage extends Component 
		implements ThermalEnergyStorageService, ValueListener, Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ThermalEnergyStorage.class);

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

	@Configuration(value="heating*")
	protected List<String> heatings;

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

	protected final List<HeatingEnergy> energyValues = new ArrayList<HeatingEnergy>();

	@Override
	public void onActivate(BundleContext context, Configurations configs) throws ComponentException {
		super.onActivate(context, configs);
		try {
			EnergyManagementService manager = context.getService(context.getServiceReference(EnergyManagementService.class));
			for (String heating : heatings) {
				ComponentService component = manager.getComponent(heating);
				if (component instanceof HeatingService) {
					logger.debug("Registering heating component \"{}\" to be feeding into thermal storage {}", heating, getId());
					
					HeatingEnergy energy = new HeatingEnergy((HeatingService) component);
					energyValues.add(energy);
				}
			}
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
			
		} catch (UnknownComponentException e) {
			throw new ConfigurationException("Unable to find controllable component " + e.getMessage());
		}
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		executor.shutdown();
	}

	@Override
	public void run() {
		try {
			long time = System.currentTimeMillis();
			double temperature;
			if (temperatures.size() > 0) {
				temperature = getAverageTemperature();
			}
			else {
				Value temp = getTemperature();
				if (temp == null) {
					return;
				}
				temperature = temp.doubleValue();
			}
			double energyInput = 0;
			for (HeatingEnergy energy : energyValues) {
				energyInput += energy.getValue().doubleValue();
			}
			if (temperatureLast != null) {
				long timeDelta = (time - temperatureLast.getTime())/1000;
				double tempDelta = temperatureLast.doubleValue() - temperature;
				
				// Calculate energy in Q[kJ] = cp*m[kg]*dT[°C]
				double energyDelta = specificHeat*mass*tempDelta;
				energyDelta = Math.max(energyDelta + energyInput, 0);
				
				energyLast += energyDelta/3600;
				energy.setLatestValue(new DoubleValue(energyLast, time));
				power.setLatestValue(new DoubleValue(energyDelta/timeDelta, time));
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
	public Value getTemperature() throws ComponentException, InvalidValueException {
		return temperature.getLatestValue();
	}

	protected double getAverageTemperature() {
		int tempCount = 0;
		double tempSum = 0;
		for (Channel channel : temperatures.values()) {
			try {
				tempSum += channel.getLatestValue().doubleValue();
				tempCount++;
				
			} catch (InvalidValueException e) {
				// Do nothing
			}
		}
		return tempSum/tempCount;
	}

	@Override
	public void onValueReceived(Value value) {
		long timeMax = -1;
		for (Channel channel : temperatures.values()) {
			try {
				Value temperatureValue = channel.getLatestValue();
				if (temperatureValue.getTime() <= timestampLast) {
					return;
				}
				if (temperatureValue.getTime() > timeMax) {
					timeMax = temperatureValue.getTime();
				}
			} catch (InvalidValueException e) {
				return;
			}
		}
		timestampLast = timeMax;
		temperature.setLatestValue(new DoubleValue(getAverageTemperature(), timestampLast));
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
