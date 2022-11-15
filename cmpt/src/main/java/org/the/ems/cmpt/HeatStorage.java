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
package org.the.ems.cmpt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.HeatingService;
import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.config.Configuration;

public abstract class HeatStorage extends Component implements ThermalEnergyStorageService {
	private final static Logger logger = LoggerFactory.getLogger(HeatStorage.class);

	@Configuration(value="heating*")
	protected List<String> heatingIds;

	protected final Map<String, HeatingService> heatings = new HashMap<String, HeatingService>();

	boolean bindHeating(HeatingService heatingService) throws ComponentException {
		String heatingId = heatingService.getId();
		if (!heatingIds.isEmpty() && !heatingIds.contains(heatingId)) {
			return false;
		}
		synchronized (heatings) {
			if (heatings.containsKey(heatingId)) {
				return false;
			}
			doBindHeating(heatingService);
		}
		return true;
	}

	private void doBindHeating(HeatingService heatingService) throws ComponentException {
		String heatingId = heatingService.getId();
		logger.info("Registering TH-E EMS {} \"{}\" to be feeding into thermal storage {}", 
				heatingService.getType().getFullName(), heatingId, getId());
		
		heatings.put(heatingId, heatingService);
		onBindHeating(heatingService);
	}

	protected void onBindHeating(HeatingService heatingService) throws ComponentException {
		// Default implementation to be overridden
	}

	boolean unbindHeating(HeatingService heatingService) {
		doUnbindHeating(heatingService);
		return true;
	}

	private void doUnbindHeating(HeatingService heatingService) {
		String heatingId = heatingService.getId();
		synchronized (heatings) {
			logger.info("Deregistered heating component: {}", heatingId);
			heatings.remove(heatingId);
			onUnbindHeating(heatingService);
		}
	}

	protected void onUnbindHeating(HeatingService heatingService) {
		// Default implementation to be overridden
	}

}
