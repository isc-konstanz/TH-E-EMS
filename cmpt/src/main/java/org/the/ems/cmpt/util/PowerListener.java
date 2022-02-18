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
package org.the.ems.cmpt.util;

import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

public class PowerListener implements ValueListener {

	protected Channel energy;
	protected Value energyLast = null;

	public PowerListener(Channel energy) {
		this.energy = energy;
	}

	public Value getEnergyValue() {
		return energyLast;
	}

	protected void onEnergyReceived(Value energyValue) {
		if (energyLast == null) {
			energyLast = energyValue;
		}
		else {
			energyLast = new DoubleValue(energyLast.doubleValue() + energyValue.doubleValue(),
										energyValue.getEpochMillis());
		}
		if (energy != null) {
			energy.setLatestValue(energyLast);
		}
	}

	protected void onPowerReceived(Value powerValue) {
		if (energyLast == null) {
			energyLast = new DoubleValue(0, powerValue.getEpochMillis());
			return;
		}
		long deltaSeconds = (powerValue.getEpochMillis() - energyLast.getEpochMillis())/1000;
		double deltaHours = (double) deltaSeconds/3600;
		Value deltaEnergy = new DoubleValue(powerValue.doubleValue()/1000/deltaHours);
		this.onEnergyReceived(deltaEnergy);
	}

	@Override
	public void onValueReceived(Value value) {
		this.onPowerReceived(value);
	}

}
