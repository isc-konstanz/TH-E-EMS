package org.the.cmpt.ctrl.mpc;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.cmpt.ctrl.mpc.Command.CommandCallbacks;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.ConfigurationHandler;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.ControlSchedule;
import org.the.ems.core.schedule.NamedThreadFactory;
import org.the.ems.core.schedule.Schedule;
import org.the.ems.core.schedule.ScheduleListener;
import org.the.ems.core.schedule.ScheduleService;

@Component(service = ScheduleService.class)
public class Control extends ConfigurationHandler
		implements ScheduleService, CommandCallbacks {

	private static final Logger logger = LoggerFactory.getLogger(Control.class);

	private static final String CONFIG_FILE = System
			.getProperty(Control.class.getPackage().getName().toLowerCase() + ".file", 
					"conf" + File.separator + "th-e-mpc.cfg");

	private ScheduledExecutorService executor;

	@Reference
	private ContentManagementService cms;

	@Configuration(mandatory=false)
	private int interval = 15;

	@Configuration(mandatory=false)
	private String python = "/usr/bin/python";

	@Configuration(mandatory=false)
	private String script = "/usr/bin/th-e-mpc";

	private Command command = null;

	private ControlSchedule schedule;

	private final List<ScheduleListener> listeners = new ArrayList<ScheduleListener>();

	@Activate
	protected void activate(ComponentContext context) {
		logger.info("Activating TH-E MPC");
		
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E MPC Pool - thread-");
		executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);
		try {
			Ini ini = new Ini(new File(CONFIG_FILE));
			
			Configurations configs = new Configurations();
			for (Entry<String, Section> section : ini.entrySet()) {
				configs.add(section.getKey(), section.getValue().entrySet());
			}
			super.onConfigure(configs);
			command = new Command(configs).register(this);
			
			LocalTime time = LocalTime.now();
			LocalTime next = time.with(next(interval)).minusMinutes(5);
			logger.debug("Starting TH-E-MPC at {}", next);
			
			executor.scheduleAtFixedRate(new ControlTask(python, script), 
					time.until(next, ChronoUnit.MILLIS), interval*60000, TimeUnit.MILLISECONDS);
			
		} catch (IOException | ConfigurationException e) {
			logger.error("Error while reading optimization configuration: {}", e.getMessage());
		}
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating TH-E MPC");
		
		command.deactivate();
		executor.shutdown();
	}

	@Override
	public void deregisterScheduleListener(ScheduleListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void registerScheduleListener(ScheduleListener listener) {
		listeners.add(listener);
	}

	@Override
	public ControlSchedule getSchedule(ScheduleListener listener) {
		listeners.add(listener);
		return getSchedule();
	}

	@Override
	public ControlSchedule getSchedule() {
		return schedule;
	}

	@Override
	public void onCommandReceived(Value power) {
		Schedule inverterSchedule = new Schedule(interval, power);
		schedule = new ControlSchedule();
		schedule.addInverterSchedule(inverterSchedule);
		
		for (ScheduleListener listener : listeners) {
			listener.onScheduleReceived(schedule);
		}
	}

	private TemporalAdjuster next(int interval) {
		return (temporal) -> {
			int minute = temporal.get(ChronoField.MINUTE_OF_DAY);
			int next = (minute / interval + 1) * interval;
			return temporal.with(ChronoField.NANO_OF_DAY, 0).plus(next, ChronoUnit.MINUTES);
		};
	}

}
