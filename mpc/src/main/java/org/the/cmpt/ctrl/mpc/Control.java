package org.the.cmpt.ctrl.mpc;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.cmpt.ctrl.mpc.Command.CommandCallbacks;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.config.ConfiguredObject;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.ControlSchedule;
import org.the.ems.core.schedule.NamedThreadFactory;
import org.the.ems.core.schedule.Schedule;
import org.the.ems.core.schedule.ScheduleListener;
import org.the.ems.core.schedule.ScheduleService;

@Component(
	service = ScheduleService.class,
	configurationPid = Control.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Control extends ConfiguredObject
		implements ScheduleService, CommandCallbacks {

	private static final Logger logger = LoggerFactory.getLogger(Control.class);

	public final static String PID = "org.the.ems.core.cmpt.mpc";

	private ScheduledExecutorService executor;

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
	public final void activate(ComponentContext context, Configurations configs) 
			throws org.osgi.service.component.ComponentException {
		
		logger.info("Activating TH-E MPC");
		try {
			super.activate((ContentManagementService) context.getBundleContext()
					.getServiceReference(ContentManagementService.class));
			
			configure(configs);
			
			NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E MPC Pool - thread-");
			executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);
			
			LocalTime time = LocalTime.now();
			LocalTime next = time.with(next(interval)).minusMinutes(5);
			logger.debug("Starting TH-E-MPC at {}", next);
			
			executor.scheduleAtFixedRate(new ControlTask(python, script), 
					time.until(next, ChronoUnit.MILLIS), interval*60000, TimeUnit.MILLISECONDS);
			
		} catch (Exception e) {
			logger.error("Error while reading optimization configuration: {}", e.getMessage());
			throw new org.osgi.service.component.ComponentException(e);
		}
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating TH-E MPC");
		
		command.deactivate();
		executor.shutdown();
	}

	@Reference(
		cardinality = ReferenceCardinality.MANDATORY,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindContentManagementService(ContentManagementService service) {
		content = service;
	}

	protected void unbindContentManagementService(ContentManagementService service) {
		content = null;
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
