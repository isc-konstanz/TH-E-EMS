package org.openmuc.framework.app.thebox.control;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

import org.apache.felix.service.command.CommandProcessor;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.ControlValueListener;
import de.thebox.control.core.component.CabinetService;
import de.thebox.control.core.component.CogeneratorService;
import de.thebox.control.core.component.ComponentException;
import de.thebox.control.core.component.ComponentService;
import de.thebox.control.core.component.HeatPumpService;
import de.thebox.control.core.component.InverterService;
import de.thebox.control.core.data.Value;

@Component(
	immediate = true,
	property = {
		CommandProcessor.COMMAND_SCOPE + ":String=th-e-control",
		CommandProcessor.COMMAND_FUNCTION + ":String=reload"
	},
	service = {
		Control.class
	}
)
public final class Control extends Thread implements ControlService {
	private final static Logger logger = LoggerFactory.getLogger(Control.class);

	private final static int SLEEP_INTERVAL = 60000;
	private final static String CONFIG_CONTROL = "de.thebox.control.config.control";
	private final static String CONFIG_COMPONENTS = "de.thebox.control.config.components";

	private final Map<String, ControlChannel> channels = new HashMap<String, ControlChannel>();

	private final Map<String, ComponentService> components = new HashMap<String, ComponentService>();
	private final Map<String, ComponentService> newComponents = new LinkedHashMap<String, ComponentService>();
	private DataAccessService access;

	private volatile boolean deactivateFlag;

	private ScheduledExecutorService scheduler = null;
	private ExecutorService executor = null;

	private Preferences configs = null;

	@Activate
	protected void activate(ComponentContext context) {
		logger.info("Activating TH-E Control");
		
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory("TH-E Control Pool - thread-");
		scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), namedThreadFactory);
		executor = Executors.newCachedThreadPool(namedThreadFactory);
		
		String fileName = System.getProperty(CONFIG_CONTROL);
		if (fileName == null) {
			fileName = "conf" + File.separator + "th-e-control.cfg";
		}
		try {
			Ini ini = new Ini(new File(fileName));
			configs = new IniPreferences(ini);
			
		} catch (IOException e) {
			logger.error("Error while reading control configuration: {}", e.getMessage());
		}
		
		start();
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating TH-E Control");
		deactivateFlag = true;

		interrupt();
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	@Reference
	protected void bindInverterService(InverterService inverterService) {
		bindComponentService(inverterService);
	}

	protected void unbindInverterService(InverterService inverterService) {
		unbindComponentService(inverterService);
	}

	@Reference
	protected void bindCogeneratorService(CogeneratorService cogeneratorService) {
		bindComponentService(cogeneratorService);
	}

	protected void unbindCogeneratorService(CogeneratorService cogeneratorService) {
		unbindComponentService(cogeneratorService);
	}

	@Reference
	protected void bindHeatPumpService(HeatPumpService heatPumpService) {
		bindComponentService(heatPumpService);
	}

	protected void unbindHeatPumpService(HeatPumpService heatPumpService) {
		unbindComponentService(heatPumpService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindCabinetService(CabinetService cabinetService) {
		bindComponentService(cabinetService);
	}

	protected void unbindCabinetService(CabinetService cabinetService) {
		unbindComponentService(cabinetService);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindComponentService(ComponentService componentService) {
		String id = componentService.getId();
		
		synchronized (newComponents) {
			if (!newComponents.containsKey(id) && !components.containsKey(id)) {
				logger.info("Registering TH-E Control {}: {}", componentService.getClass().getInterfaces()[0].getSimpleName(), id);
				
				newComponents.put(id, componentService);
				interrupt();
			}
		}
	}

	protected void unbindComponentService(ComponentService componentService) {
		String id = componentService.getId();
		
		// OSGi deactivate functions are always called sequentially:
		ComponentService removedComponent;
		synchronized (components) {
			removedComponent = components.remove(id);
		}
		if (removedComponent != null) {
			logger.info("Deregistering TH-E Control component: " + id);
			
			removedComponent.deactivate();
		}
		else {
			// Component was removed before it was added to the active components
			newComponents.remove(id);
		}
		logger.debug("Component deregistered: " + id);
	}

	@Reference
	protected void bindDataAccessService(DataAccessService dataAccessService) {
		this.access = dataAccessService;
	}

	protected void unbindDataAccessService(DataAccessService dataAccessService) {
		this.access = null;
	}

	public void reload() {
		logger.info("Reload TH-E Control configuration.");
		// TODO: implement reload
		String fileName = System.getProperty(CONFIG_CONTROL);
		if (fileName == null) {
			fileName = "conf" + File.separator + "th-e-control.cfg";
		}
		try {
			Ini ini = new Ini(new File(fileName));
			configs = new IniPreferences(ini);
			
		} catch (IOException e) {
			logger.error("Error while reloading TH-E Control configuration: {}", e.getMessage());
		}
	}

	@Override
	public Preferences readComponentConfigs(String component) throws IOException {
		String fileDir = System.getProperty(CONFIG_COMPONENTS);
		if (fileDir == null) {
			fileDir = "conf" + File.separator + "components";
		}
		if (!fileDir.endsWith(File.separator)) {
			fileDir += File.separator;
		}
		String fileName = fileDir + component.toLowerCase() + ".cfg";
		Ini ini = new Ini(new File(fileName));
		return new IniPreferences(ini);
	}

	@Override
	public boolean writeValue(String id, Value value) {
		logger.debug("Writing value for channel \"{}\": {}", id, value);
		
		ControlChannel channel = getChannel(id);
		if (channel != null) {
			return channel.write(value);
		}
		return false;
	}

	@Override
	public void setLatestValue(String id, Value value) {
		logger.debug("Set latest value for channel \"{}\": {}", id, value);
		
		ControlChannel channel = getChannel(id);
		if (channel != null) {
			channel.setLatestValue(value);
		}
	}

	@Override
	public Value getLatestValue(String id) {
		ControlChannel channel = getChannel(id);
		if (channel != null) {
			return channel.getLatestValue();
		}
		return null;
	}

	@Override
	public Value getLatestValue(String id, ControlValueListener listener) {
		ControlChannel channel = getChannel(id);
		if (channel != null) {
			channel.register(listener);
			
			return channel.getLatestValue();
		}
		return null;
	}

	@Override
	public void registerValueListener(String id, ControlValueListener listener) {
		ControlChannel channel = getChannel(id);
		if (channel != null) {
			channel.register(listener);
		}
	}

	@Override
	public void deregisterValueListener(String id, ControlValueListener listener) {
		ControlChannel channel = getChannel(id);
		if (channel != null) {
			channel.deregister(listener);
		}
	}

	private ControlChannel getChannel(String id) {
		if (!channels.containsKey(id)) {
			if (!access.getAllIds().contains(id)) {
				return null;
			}
			ControlChannel channel = new ControlChannel(access.getChannel(id));
			channels.put(id, channel);
			
			return channel;
		}
		return channels.get(id);
	}

	@Override
	public void run() {
		logger.info("Starting TH-E Control");
		
		setName("TH-E Control");
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
			logger.info("TH-E Control thread interrupted and will stop");
			executor.shutdown();
			scheduler.shutdown();
			return;
		}
		
		if (access != null) {
			synchronized (newComponents) {
				if (newComponents.size() != 0) {
					synchronized (components) {
						components.putAll(newComponents);
					}
					for (Entry<String, ComponentService> newComponentEntry : newComponents.entrySet()) {
						String id = newComponentEntry.getKey();
						logger.info("Activating TH-E Control component: " + id);
						try {
							ComponentService component = newComponentEntry.getValue();
							
							component.activate(this);
							if (component instanceof InverterService) {
								onInverterActivated((InverterService) component);
							}
							else if (component instanceof CogeneratorService) {
								onCogeneratorActivated((CogeneratorService) component);
							}
							else if (component instanceof HeatPumpService) {
								onHeatPumpActivated((HeatPumpService) component);
							}
						} catch (ComponentException e) {
							logger.warn("Error while activating component \"{}\": ", id, e);
						}
					}
					newComponents.clear();
				}
			}
			
			// TODO: handle control schedule
		}
	}

	private void onInverterActivated(InverterService interver) {
		// TODO: handle inverter activation
		
	}

	private void onCogeneratorActivated(CogeneratorService cogenerator) {
		// TODO: handle cogenerator activation
		
	}

	private void onHeatPumpActivated(HeatPumpService heatpump) {
		// TODO: handle heatpump activation
		
	}
}
