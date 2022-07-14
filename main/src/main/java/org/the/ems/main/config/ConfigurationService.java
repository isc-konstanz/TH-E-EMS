/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.the.ems.main.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentType;
import org.the.ems.core.ContentManagementService;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.main.EnergyManager;

@Component(service = ConfigurationService.class, immediate = true)
public final class ConfigurationService extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

	private final static String CONFIG_DIR_DEFAULT = "conf" + File.separator + "components" + File.separator;
	private final static String CONFIG_DIR = System.getProperty("org.the.ems.config", CONFIG_DIR_DEFAULT);

	private final Path dir = Paths.get(CONFIG_DIR);

	private File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".cfg"));

	private Map<WatchKey, Path> keys = new HashMap<WatchKey, Path>();
	private Map<String, Long> times = new HashMap<String, Long>();

	private WatchService watcher;

	@Reference
	ConfigurationAdmin configs;

	@Reference
	ContentManagementService content; // make sure the CMS is active before registering bundles

	@Activate
	protected void activate(BundleContext context) {
		try {
			watcher = FileSystems.getDefault().newWatchService();
			load(EnergyManager.PID, ConfigurationReader.read(
					EnergyManager.ID, dir.resolve(EnergyManager.ID + ".cfg").toFile()));
			
			register(dir);
			start();
			
		} catch (Exception e) {
			logger.error("Error while initializing configurations: {}", e.getMessage());
		}
	}

	@Override
	public void run() {
		logger.debug("Starting TH-E Configuration");
		try {
			if (files == null || files.length <= 1) {
				return;
			}
			wait(1);
			
			// FIXME: maybe use BundleListener to see if a bundle with the configured PID started
			// and only continue if all bundles are active, instead of waiting a second
			
			register(ComponentType.GENERAL);
			register(ComponentType.ELECTRIC_VEHICLE, ComponentType.VENTILATION);
			register(ComponentType.ELECTRICAL_ENERGY_STORAGE, 
					ComponentType.THERMAL_ENERGY_STORAGE);
			
			wait(1);
			register(ComponentType.COMBINED_HEAT_POWER, 
					ComponentType.HEAT_PUMP, 
					ComponentType.HEATING_ROD);
			register(ComponentType.INVERTER);
			
			wait(1);
			register(ComponentType.CONTROL);
			
		} catch (Exception e) {
			logger.error("Error while initializing configurations: {}", e.getMessage());
		}
	}

	private void wait(int seconds) {
		try {
			Thread.sleep(seconds*1000);
			
		} catch (InterruptedException e) {
		}
	}

	public void watch() {
		try {
			WatchKey key = watcher.poll();
			if (key == null || !keys.containsKey(key)) {
				return;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				
				// This key is registered only for ENTRY_CREATE events,
				// but an OVERFLOW event can occur regardless if events
				// are lost or discarded.
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}
				Path path = keys.get(key);
				File file;
				if (Files.isSameFile(path, dir)) {
					file = path.resolve((Path) event.context()).toFile();
				}
				else {
					file = path.toFile();
				}
				if (times.containsKey(file.getPath()) && times.get(file.getPath()) <= file.lastModified()) {
					continue;
				}
				times.put(file.getPath(), file.lastModified());
				
				load(file);
			}
			key.reset();
			
		} catch (ConfigurationException | IOException e) {
			logger.debug("Error while watching configuration updates");
		}
	}

	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_MODIFY,
				StandardWatchEventKinds.ENTRY_DELETE);
		
		keys.put(key, dir);
	}

	private void register(ComponentType... types) throws Exception {
		for (ComponentType type : types) {
			files:
			for (File file : files) {
				String id = file.getName();
				int pos = id.lastIndexOf(".");
				if (pos > 0) {
					id = id.substring(0, pos);
				}
				if (id.equals(EnergyManager.ID)) {
					continue;
				}
				if (!id.startsWith(type.getKey())) {
					if (type != ComponentType.GENERAL) {
						continue;
					}
					else {
						for (ComponentType t : ComponentType.values()) {
							if (id.startsWith(t.getKey())) {
								continue files;
							}
						}
					}
				}
				Path dir = this.dir.resolve(id.concat(".d"));
				if (dir.toFile().isDirectory()) {
					register(dir);
				}
				load(file, id, type);
			}
		}
	}

	private void load(File file) throws ConfigurationException {
		String id = file.getName();
		int pos = id.lastIndexOf(".");
		if (pos > 0) {
			id = id.substring(0, pos);
		}
		if (id.equals(EnergyManager.ID)) {
			return;
		}
		ComponentType type = ComponentType.GENERAL;
		for (ComponentType t : ComponentType.values()) {
			if (id.startsWith(t.getKey())) {
				type = t;
				break;
			}
		}
		load(file, id, type);
	}

	private void load(File file, String id, ComponentType type) throws ConfigurationException {
		ConfigurationReader configs = ConfigurationReader.read(id, dir.resolve(id.concat(".cfg")).toFile());

		File[] dir = this.dir.resolve(id+".d").toFile().listFiles((d, name) -> name.endsWith(".cfg"));
		if (dir != null && dir.length > 0) {
			for (File opt : dir) {
				configs.configure(opt);
			}
		}
		if (id.startsWith(type.getKey())) {
			String pid = type.getId();
			if (configs.containsKey(ConfigurationReader.GENERAL, "type")) {
				pid = pid.concat(".")
						.concat(configs.get(ConfigurationReader.GENERAL, "type").toLowerCase()
																				.replaceAll("[^a-zA-Z0-9]", "."));
			}
			
			String alias = id.substring(type.getKey().length());
			if (alias.length() == 0) {
				alias = "0";
			}
			load(pid, alias, configs);
			return;
		}
		if (configs.containsKey(ConfigurationReader.GENERAL, "pid")) {
			String pid = configs.get(ConfigurationReader.GENERAL, "pid");
			load(pid, configs);
			return;
		}
		throw new ConfigurationException("Missing PID for component configuration: "+file.getName());
	}

	private void load(String pid, String alias, ConfigurationReader configs) throws ConfigurationException {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading component {} {}", pid.concat("~").concat(alias), configs);
		}
		try {
			this.configs.getFactoryConfiguration(pid, alias, "?").update(configs);
			
		} catch (IllegalArgumentException | IOException e) {
			throw new ConfigurationException(e);
		}
	}

	private void load(String pid, ConfigurationReader configs) throws ConfigurationException {
		logger.debug("Loading component {} {}", pid, configs);
		try {
			this.configs.getConfiguration(pid, "?").update(configs);
			
		} catch (IllegalArgumentException | IOException e) {
			throw new ConfigurationException(e);
		}
	}

}
