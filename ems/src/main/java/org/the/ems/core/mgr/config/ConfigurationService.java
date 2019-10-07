/* 
 * Copyright 2016-19 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
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
package org.the.ems.core.mgr.config;

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

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentType;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.mgr.EnergyManager;

@Component(service = ConfigurationService.class, immediate = true)
public final class ConfigurationService {
	private final static Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

	private final static String CONFIG_DIR_DEFAULT = "conf" + File.separator + "ems" + File.separator;
	private final static String CONFIG_DIR = System.getProperty("org.the.ems.core.config", CONFIG_DIR_DEFAULT);

	private final Path dir = Paths.get(CONFIG_DIR);

	private Map<WatchKey, Path> keys = new HashMap<WatchKey, Path>();
	private Map<String, Long> times = new HashMap<String, Long>();

	private WatchService watcher;

	@Reference
	ConfigurationAdmin configs;

	@Activate
	protected void activate() {
		try {
			watcher = FileSystems.getDefault().newWatchService();
			register(dir);
			
			load(EnergyManager.PID, Configurations.create(
					EnergyManager.ID, 
					dir.resolve(EnergyManager.ID + ".cfg").toFile()));
			
			File[] files = this.dir.toFile().listFiles((d, name) -> name.endsWith(".cfg"));
			if (files == null || files.length < 1) {
				return;
			}
			for (File file : files) {
				String id = file.getName();
				int pos = id.lastIndexOf(".");
				if (pos > 0) {
					id = id.substring(0, pos);
				}
				Path dir = this.dir.resolve(id.concat(".d"));
				if (dir.toFile().isDirectory()) {
					register(dir);
				}
				load(file);
			}
		} catch (ConfigurationException | IOException e) {
			logger.error("Error while initializing configurations: {}", e.getMessage());
		}
	}

	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_MODIFY,
				StandardWatchEventKinds.ENTRY_DELETE);
		
		keys.put(key, dir);
	}

	public void watch() {
		try {
			WatchKey key = watcher.take();
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
			
		} catch (InterruptedException | ConfigurationException | IOException e) {
			logger.debug("Error while watching configuration updates");
		}
	}

	private void load(File file) throws ConfigurationException {
		String id = file.getName();
		int pos = id.lastIndexOf(".");
		if (pos > 0) {
			id = id.substring(0, pos);
		}
		Configurations configs = Configurations.create(id, dir.resolve(id.concat(".cfg")).toFile());
		
		File[] dir = this.dir.resolve(id+".d").toFile().listFiles((d, name) -> name.endsWith(".cfg"));
		if (dir != null && dir.length > 0) {
			for (File opt : dir) {
				configs.configure(opt);
			}
		}
		for (ComponentType type : ComponentType.values()) {
			if (id.startsWith(type.getKey())) {
				String pid = type.getId();
				if (configs.contains(Configurations.GENERAL, "type")) {
					pid = pid.concat(".")
							.concat(configs.get(Configurations.GENERAL, "type").toLowerCase());
				}
				
				String alias = id.substring(type.getKey().length());
				if (alias.length() == 0) {
					alias = "0";
				}
				load(pid, alias, configs);
				return;
			}
		}
		if (!id.equals(EnergyManager.ID)) {
			if (configs.contains(Configurations.GENERAL, "pid")) {
				String pid = configs.get(Configurations.GENERAL, "pid");
				load(pid, configs);
				return;
			}
		}
		throw new ConfigurationException("Missing PID for component configuration: "+file.getName());
	}

	private void load(String pid, String alias, Configurations configs) throws ConfigurationException {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading component {} {}", pid.concat("~").concat(alias), configs);
		}
		try {
			this.configs.getFactoryConfiguration(pid, alias, "?").update(configs);
			
		} catch (IllegalArgumentException | IOException e) {
			throw new ConfigurationException(e);
		}
	}

	private void load(String pid, Configurations configs) throws ConfigurationException {
		logger.debug("Loading component {} {}", pid, configs);
		try {
			this.configs.getConfiguration(pid, "?").update(configs);
			
		} catch (IllegalArgumentException | IOException e) {
			throw new ConfigurationException(e);
		}
	}

}
