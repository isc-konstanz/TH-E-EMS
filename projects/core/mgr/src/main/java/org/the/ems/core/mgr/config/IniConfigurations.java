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
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;

public class IniConfigurations extends Configurations {

	public IniConfigurations(String fileName) throws ConfigurationException {
		try {
			Ini ini = new Ini(new File(fileName));
			
			for (Entry<String, Section> section : ini.entrySet()) {
				add(section.getKey(), section.getValue().entrySet());
			}
		} catch (IOException e) {
			throw new ConfigurationException("Error while reading configuration: " + e.getMessage());
		}
	}

}
