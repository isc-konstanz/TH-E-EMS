package de.thebox.control.opt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.schedule.ControlSchedule;
import de.thebox.control.core.schedule.NamedThreadFactory;
import de.thebox.control.core.schedule.ScheduleListener;
import de.thebox.control.core.schedule.ScheduleService;
import de.thebox.control.feature.emoncms.EmoncmsConfig;
import de.thebox.control.opt.service.GridService;
import de.thebox.control.opt.service.GridServiceConfig;

@Component
public class Optimization extends Thread implements ScheduleService {
	private final static Logger logger = LoggerFactory.getLogger(Optimization.class);

	private final static int SLEEP_INTERVAL = 60000;
	private final static String CONFIG_OPTIMIZATION = "de.thebox.control.config.optimization";

	private final List<ScheduleListener> listeners = new ArrayList<ScheduleListener>();

	private GridService gridService = null;

	private volatile boolean deactivateFlag;
	private ExecutorService executor = null;

	private Preferences config = null;
//	private int interval;

	@Activate
	protected void activate(ComponentContext context) {
		logger.info("Activating TH-E Optimization");
		
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E Optimization Pool - thread-");
		executor = Executors.newCachedThreadPool(namedThreadFactory);
		
		String fileName = System.getProperty(CONFIG_OPTIMIZATION);
		if (fileName == null) {
			fileName = "conf" + File.separator + "th-e-optimization.cfg";
		}
		try {
			Ini ini = new Ini(new File(fileName));
			config = new IniPreferences(ini);
//			OptimizationConfig config = new OptimizationConfig(prefs);
//			
//			interval = config.getInterval();
			
		} catch (IOException e) {
			logger.error("Error while reading optimization configuration: {}", e.getMessage());
		}
		
		start();
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating TH-E Optimization");
		deactivateFlag = true;
		
		if (gridService != null) {
			gridService.deactivate();
		}
		interrupt();
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void registerScheduleListener(ScheduleListener listener) {
		try {
			if (config.nodeExists(GridServiceConfig.SECTION) && config.nodeExists(EmoncmsConfig.SECTION)) {
				gridService = new GridService((ControlService) listener, config);
			}
		} catch (ComponentException | BackingStoreException e) {
			logger.error("Error while activating grid service: {}", e.getMessage());
		}
		listeners.add(listener);
	}

	@Override
	public void deregisterScheduleListener(ScheduleListener listener) {
		listeners.remove(listener);
	}

	@Override
	public ControlSchedule getSchedule(ScheduleListener listener) {
		registerScheduleListener(listener);
		return getSchedule();
	}

	@Override
	public ControlSchedule getSchedule() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		logger.info("Starting TH-E Optimization");
		
		setName("TH-E Optimization");
		handleInterruptEvent();
		
		while (!deactivateFlag) {
			if (interrupted()) {
				handleInterruptEvent();
				continue;
			}
			try {
				while (true) {
					// TODO: implement channel flag checks, component and optimization verifications
					
					Thread.sleep(SLEEP_INTERVAL);
				}
			} catch (InterruptedException e) {
				handleInterruptEvent();
				continue;
			}
		}
	}

	private void handleInterruptEvent() {
		if (deactivateFlag) {
			logger.info("TH-E Optimization thread interrupted and will stop");
			executor.shutdown();
			return;
		}
	}

}
