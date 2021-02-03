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
package org.the.ems.cmpt.inv.ext;

import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class PowerListener implements ValueListener {

	/**
	 * Interface used to notify the {@link ConsumptionPower} 
	 * implementation about changed power values
	 */
	public interface PowerCallbacks {
		public void onPowerValueReceived(PowerType type, Value power);
	}

	/**
	 * The Listeners' current callback object, which is notified of changed power values
	 */
	private final PowerCallbacks callbacks;

	private final PowerType type;

	public PowerListener(PowerCallbacks callbacks, PowerType type) {
		this.callbacks = callbacks;
		this.type = type;
	}

	public PowerType getType() {
		return type;
	}

	@Override
	public void onValueReceived(Value value) {
		callbacks.onPowerValueReceived(type, value);
	}
}
