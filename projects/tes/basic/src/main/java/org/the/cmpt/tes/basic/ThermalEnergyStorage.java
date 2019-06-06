package org.the.cmpt.tes.basic;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.cmpt.ConfiguredComponent;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.NamedThreadFactory;

@Component
public class ThermalEnergyStorage extends ConfiguredComponent
		implements ThermalEnergyStorageService, Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ThermalEnergyStorage.class);

	private final static String ID = "Basic";

	private ScheduledExecutorService executor;

	@Configuration(mandatory=false)
	private int interval = 15;

	// The specific heat capacity of the storage medium. Default is 4.1813 of water.
	@Configuration(mandatory=false)
	protected double specificHeat = 4.1813;

	// The density of the storage medium. Default is 1 of water.
	@Configuration(mandatory=false)
	protected double density = 1;

	@Configuration
	protected double capacity;

	protected double mass;

	@Configuration(mandatory=false, value={"temp_h10", "temp_h30", "temp_h60", "temp_h90"})
	protected ChannelCollection temperatures;

	protected Value temperatureLast = null;

	@Configuration
	protected Channel power;

	@Configuration
	protected Channel energy;
	protected double energyLast = 0;

	protected final List<GeneratorEnergy> energyValues = new ArrayList<GeneratorEnergy>();

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		
		// Storage medium mass in kilogram
		mass = capacity*density;
		
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E EMS "+ID+" TES Pool - thread-");
		executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);
		
		LocalTime time = LocalTime.now();
		LocalTime next = time.with(next(interval)).minusMinutes(5);
		logger.debug("Starting TH-E EMS {} TES power calculation {}", ID, next);
		
		executor.scheduleAtFixedRate(this, 
				time.until(next, ChronoUnit.MILLIS), interval*60000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onDeactivate() throws EnergyManagementException {
		super.onDeactivate();
		executor.shutdown();
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindCogeneratorService(CogeneratorService service) {
		GeneratorEnergy energy = new GeneratorEnergy(service);
		energyValues.add(energy);
	}

	protected void unbindCogeneratorService(CogeneratorService service) {
		unbindComponentSerivce(service);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindHeatPumpService(HeatPumpService service) {
		GeneratorEnergy energy = new GeneratorEnergy(service);
		energyValues.add(energy);
	}

	protected void unbindHeatPumpService(HeatPumpService service) {
		unbindComponentSerivce(service);
	}

	private void unbindComponentSerivce(ComponentService service) {
		ListIterator<GeneratorEnergy> iter = energyValues.listIterator();
		while(iter.hasNext()){
		    if(iter.next().getService().getId().equals(service.getId())){
		        iter.remove();
		    }
		}
	}

	@Override
	public void run() {
		try {
			Value temperature = getTemperature();
			if (temperatureLast != null && temperature.getTime() > temperatureLast.getTime()) {
				long timeDelta = (temperature.getTime() - temperatureLast.getTime())/1000;
				double tempDelta = temperatureLast.doubleValue() - temperature.doubleValue();
				
				// Calculate energy in Q[kJ] = cp*m[kg]*dT[°C]
				double energyDelta = specificHeat*mass*tempDelta;
				
				for (GeneratorEnergy energy : energyValues) {
					energyDelta -= energy.getValue().doubleValue();
				}
				
				energyLast += energyDelta/3600;
				energy.setLatestValue(new DoubleValue(energyLast, temperature.getTime()));
				power.setLatestValue(new DoubleValue(energyDelta/timeDelta, temperature.getTime()));
			}
			temperatureLast = temperature;
			
		} catch (ComponentException e) {
			logger.warn("Error calculating power: {}", e.getMessage());
		}
	}

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	public Value getThermalPower() throws ComponentException {
		return power.getLatestValue();
	}

	@Override
	public Value getTemperature() throws ComponentException {
		long time = System.currentTimeMillis();
		double sum = 0;
		for (Channel temperature : temperatures.values()) {
			Value val = temperature.getLatestValue();
			sum += val.doubleValue();
//			time = Math.min(time, val.getTime());
		}
		return new DoubleValue(sum/temperatures.size(), time);
	}

	private TemporalAdjuster next(int interval) {
		return (temporal) -> {
			int minute = temporal.get(ChronoField.MINUTE_OF_DAY);
			int next = (minute / interval + 1) * interval;
			return temporal.with(ChronoField.NANO_OF_DAY, 0).plus(next, ChronoUnit.MINUTES);
		};
	}

}
