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
import java.util.Map.Entry;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.config.ConfigurationException;

public class Configurations extends org.the.ems.core.config.Configurations {
    private static final Logger logger = LoggerFactory.getLogger(Configurations.class);

	public Configurations configure(File[] files) throws ConfigurationException {
		if (files != null && files.length > 0) {
			for (File file : files) {
				configure(file);
			}
		}
		return this;
	}

	public Configurations configure(File file) throws ConfigurationException {
		if (file.exists()) {
			logger.debug("Configuring from file: {}", file.getAbsolutePath());
			try {
				Ini ini = new Ini(file);
				
				for (Entry<String, Section> section : ini.entrySet()) {
					for (Entry<String, String> value : section.getValue().entrySet()) {
						if (logger.isDebugEnabled() && contains(section.getKey(), value.getKey())) {
							logger.debug("Overriding settings \"{}\": {}", section.getKey(), value.getValue());
						}
						if (!section.getKey().toLowerCase().equals("general")) {
							add(section.getKey(), value.getKey(), value.getValue());
						}
						else {
							add(Configurations.GENERAL, value.getKey(), value.getValue());
						}
					}
				}
			} catch (IOException e) {
				throw new ConfigurationException("Error while reading configuration: " + e.getMessage());
			}
		}
		return this;
	}

	public static Configurations create(File file) throws ConfigurationException {
		if (!file.exists()) {
			new ConfigurationException("Component configuration does not exist: " + file.getName());
		}
		return new Configurations().configure(file);
	}

	public static Configurations create(String id, File file) throws ConfigurationException {
		Configurations configurations = create(file);
		configurations.add(org.the.ems.core.config.Configurations.GENERAL, "id", id);
		
		return configurations;
	}

}
