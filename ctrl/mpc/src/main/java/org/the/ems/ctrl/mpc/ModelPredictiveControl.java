package org.the.ems.ctrl.mpc;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;
import org.the.ems.core.schedule.ControlSchedule;
import org.the.ems.core.schedule.NamedThreadFactory;
import org.the.ems.core.schedule.Schedule;
import org.the.ems.core.schedule.ScheduleListener;
import org.the.ems.core.schedule.ScheduleService;
import org.the.ems.ctrl.mpc.Command.CommandCallbacks;

@org.osgi.service.component.annotations.Component(
	service = ScheduleService.class,
	configurationPid = ModelPredictiveControl.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ModelPredictiveControl extends Component
		implements ScheduleService, CommandCallbacks {

	private static final Logger logger = LoggerFactory.getLogger(ModelPredictiveControl.class);

	public final static String PID = "org.the.ems.ctrl.mpc";

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

	@Override
	public ComponentType getType() {
		return ComponentType.CONTROL;
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		logger.info("Activating TH-E MPC");
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E MPC Pool - thread-");
		executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);
		
		LocalTime time = LocalTime.now();
		LocalTime next = time.with(next(interval)).minusMinutes(5);
		logger.debug("Starting TH-E-MPC at {}", next);
		
		executor.scheduleAtFixedRate(new ModelPredictionTask(python, script), 
				time.until(next, ChronoUnit.MILLIS), interval*60000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
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
			return temporal.with(ChronoField.SECOND_OF_DAY, 0)
					.with(ChronoField.MILLI_OF_DAY, 0)
					.with(ChronoField.NANO_OF_DAY, 0)
					.plus(next, ChronoUnit.MINUTES);
		};
	}

}
