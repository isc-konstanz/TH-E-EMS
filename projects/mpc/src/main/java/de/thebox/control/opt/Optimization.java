package de.thebox.control.opt;

//@Component(
//	immediate = true,
//	service = {
//		Optimization.class
//	}
//)
public class Optimization {
//public class Optimization implements ScheduleService, GridServiceCallbacks {
//	private final static Logger logger = LoggerFactory.getLogger(Optimization.class);
//
//	private final static String CONFIG_OPTIMIZATION = "de.thebox.control.config.optimization";
//
//	private final List<ScheduleListener> listeners = new ArrayList<ScheduleListener>();
//
//	private GridService gridService = null;
//
//	private ScheduledExecutorService executor;
//	private Preferences config;
//
//	private ControlSchedule schedule;
//
//	private int interval;
//
//	@Activate
//	protected void activate(ComponentContext context) {
//		logger.info("Activating TH-E Optimization");
//		
//		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E Optimization Pool - thread-");
//		executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);
//		
//		String fileName = System.getProperty(CONFIG_OPTIMIZATION);
//		if (fileName == null) {
//			fileName = "conf" + File.separator + "th-e-optimization.cfg";
//		}
//		try {
//			Ini ini = new Ini(new File(fileName));
//			config = new IniPreferences(ini);
//			
//			OptimizationConfig config = new OptimizationConfig(this.config);
//			interval = config.getInterval();
//			LocalTime time = LocalTime.now();
//			LocalTime next = time.with(next(interval)).minusMinutes(5);
//			logger.debug("Starting TH-E-Optimization at {}", next);
//			
//		    executor.scheduleAtFixedRate(new OptimizationTask(config.getPython(), config.getScript()), 
//		    		time.until(next, ChronoUnit.MILLIS), interval*60000, TimeUnit.MILLISECONDS);
//			
//		} catch (IOException | ConfigurationException e) {
//			logger.error("Error while reading optimization configuration: {}", e.getMessage());
//		}
//	}
//
//	@Deactivate
//	protected void deactivate(ComponentContext context) {
//		logger.info("Deactivating TH-E Optimization");
//		
//		if (gridService != null) {
//			gridService.deactivate();
//		}
//		executor.shutdown();
//	}
//
//	@Override
//	public void registerScheduleListener(ScheduleListener listener) {
//		try {
//			if (config.nodeExists(GridServiceConfig.SECTION) && config.nodeExists(RemoteConfig.SECTION)) {
//				gridService = new GridService(this, (ControlService) listener, config);
//			}
//		} catch (ComponentException | BackingStoreException e) {
//			logger.error("Error while activating grid service: {}", e.getMessage());
//		}
//		listeners.add(listener);
//	}
//
//	@Override
//	public void deregisterScheduleListener(ScheduleListener listener) {
//		listeners.remove(listener);
//	}
//
//	@Override
//	public ControlSchedule getSchedule(ScheduleListener listener) {
//		registerScheduleListener(listener);
//		return getSchedule();
//	}
//
//	@Override
//	public ControlSchedule getSchedule() {
//		return schedule;
//	}
//
//	@Override
//	public void onGridServiceRequest(Value power) {
//		Schedule inverterSchedule = new Schedule(interval, power);
//		schedule = new ControlSchedule();
//		schedule.addInverterSchedule(inverterSchedule);
//		
//		for (ScheduleListener listener : listeners) {
//			listener.onScheduleReceived(schedule);
//		}
//	}
//
//	private TemporalAdjuster next(int interval) {
//		return (temporal) -> {
//			int minute = temporal.get(ChronoField.MINUTE_OF_DAY);
//		    int next = (minute / interval + 1) * interval;
//		    return temporal.with(ChronoField.NANO_OF_DAY, 0).plus(next, ChronoUnit.MINUTES);
//		};
//	}
//
}
