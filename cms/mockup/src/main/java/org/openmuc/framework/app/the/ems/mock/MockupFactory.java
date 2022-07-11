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
package org.openmuc.framework.app.the.ems.mock;

import java.util.Arrays;
import java.util.List;

import org.openmuc.framework.app.the.ems.ChannelFactory;
import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.config.RootConfig;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
	immediate = true,
	service = ChannelFactory.class
)
public class MockupFactory implements ChannelFactory {
	private final static Logger logger = LoggerFactory.getLogger(MockupFactory.class);

	private final static List<String> SEPARATORS = Arrays.asList("-", "_", ".", ":" ,"/");
	private final static String VIRTUAL = "virtual";

	@Reference
	private ConfigService configs;

	@Override
	public void newChannel(String id) {
		String separator = "_";
	    for (int i = 0; i < id.length(); i++) {
	    	String ch = String.valueOf(id.charAt(i));
	        if (SEPARATORS.contains(ch)) {
        		separator = ch;
        		break;
	        }
	    }
		String did = id.split(separator)[0];
		try {
			boolean update = false;
			RootConfig root = configs.getConfig();
			DriverConfig driver = root.getDriver(VIRTUAL);
			if (driver == null) {
				driver = root.addDriver(VIRTUAL);
				update = true;
			}
			DeviceConfig device = driver.getDevice(did);
			if (device == null) {
				device = driver.addDevice(did);
				update = true;
			}
			ChannelConfig configs = device.getChannel(id);
			if (configs == null) {
				configs = device.addChannel(id);
				configs.setDescription(ChannelConfig.DESCRIPTION_DEFAULT);
				configs.setValueType(ChannelConfig.VALUE_TYPE_DEFAULT);
				configs.setUnit(ChannelConfig.UNIT_DEFAULT);
				update = true;
			}
			if (update) {
	            this.configs.setConfig(root);
	            this.configs.writeConfigToFile();
			}
		} catch (IdCollisionException | ConfigWriteException e) {
			logger.warn("Unable to instantiate channel for id: " + id);
		}
	}

}
