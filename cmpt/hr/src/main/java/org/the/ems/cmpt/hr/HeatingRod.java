/* 
 * Copyright 2016-21 ISC Konstanz
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
package org.the.ems.cmpt.hr;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.cmpt.Heating;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.HeatingRodService;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = HeatingRodService.class,
	configurationPid = HeatingRodService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class HeatingRod extends Heating implements HeatingRodService {

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		container.add(state, new BooleanValue(true, value.getTime()));
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		container.add(state, new BooleanValue(false, time));
	}

}
