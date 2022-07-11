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
package org.the.ems.ctrl.ps;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentType;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.Control;
import org.the.ems.ctrl.PidControl;

//FIXME: Reintroduced "Control" as parent class for PeakShavingControl
@Component(scope = ServiceScope.BUNDLE, configurationPid = Control.PID
		+ ".ps", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PeakShavingControl extends Control {
	private final static Logger logger = LoggerFactory.getLogger(PeakShavingControl.class);

	@Configuration(mandatory = false, scale = -1000)
	protected double exportMax = Double.NEGATIVE_INFINITY;

	@Configuration(mandatory = false, scale = 1000)
	protected double exportHyst = 0;

	@Configuration(mandatory = false, scale = 1000)
	protected double importMax = 0;

	@Configuration(mandatory = false)
	protected Channel setpoint;

	@Configuration
	protected Channel power;

	@Configuration(mandatory=false, value={"power_limit", "power_limitation"})
	protected Channel powerLimit;

	@Configuration(mandatory=false)
	protected double powerScale = 1;

	@Configuration(mandatory = false)
	protected int powerSamples = 5;

	@Configuration
	protected Channel powerSetpoint;

	protected Value powerValue;
	protected Value powerLimitValue;
	protected PidControl control;

	protected double soc = 0;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		power.registerValueListener(new PowerListener());
		powerLimit.registerValueListener(new PowerLimitListener());

		control = new PidControl().configure(configs);

		powerValue = DoubleValue.zeroValue();
		powerLimitValue = new DoubleValue(importMax);
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		power.deregisterValueListeners();
		powerLimit.deregisterValueListeners();
	}

	protected void set(Value value) {
		double power = value.doubleValue();
		long time = Math.max(powerLimitValue.getEpochMillis(), powerValue.getEpochMillis());
		double error = 0;

		double controlValue;
		try {
			if (power < 0) {
				error = getPowerExportMax() - power;
			}
			else if (power >= 0) {
				error = getPowerImportMax() - power;
			}
			controlValue = control.process(time, error);
			if (controlValue != powerSetpoint.getLatestValue().doubleValue()) {
				onControlChanged(new DoubleValue(-controlValue, time));
			}
		} catch (InvalidValueException e) {
			logger.warn("Obligatory inverter value missing: {}", e.getMessage());
		}
	}

	protected void onControlChanged(Value value) {
		if (value.doubleValue() > 0 && inverters.hasDischargableStorage()) {
			this.inverters.set(value);
		}
		else {
			this.inverters.set(new DoubleValue(0, value.getEpochMillis()));
		}
	}

	protected double getPowerImportMax() {
		if (!Double.isNaN(powerLimitValue.doubleValue())) {
			return Math.min(powerLimitValue.doubleValue(), importMax);
		}
		return importMax;

	}

	protected double getPowerExportMax() {
		if (!Double.isNaN(powerLimitValue.doubleValue())) {
			return Math.max(-powerLimitValue.doubleValue(), exportMax);
		}
		return exportMax;
	}

	protected void onPowerChanged(Value value) {
		double power = value.doubleValue();

		if ((power >= getPowerImportMax() || power >= getPowerExportMax() + exportHyst)
				&& heatings.hasStoppable(ComponentType.HEATING_ROD, ComponentType.HEAT_PUMP)) {
			heatings.stopFirst(ComponentType.HEATING_ROD, ComponentType.HEAT_PUMP);
			logger.debug("Stopping electrical heating due to power boundary infringement: {}", power);

//		} else if (power >= getPowerImportMax() && heatings.hasStartable(ComponentType.COMBINED_HEAT_POWER)) {
//			FIXME: uncomment following lines for original peak-shaving
//			heatings.startFirst(ComponentType.COMBINED_HEAT_POWER);
//			logger.debug("Starting cogeneration due to power boundary infringement: {}", power);

		} else if (power <= getPowerExportMax() && heatings.hasStoppable(ComponentType.COMBINED_HEAT_POWER)) {
			heatings.stopFirst(ComponentType.COMBINED_HEAT_POWER);
			logger.debug("Stopping cogeneration due to power boundary infringement: {}", power);

		} else if (power <= getPowerExportMax() && power + heatings.getStartableMinPower(ComponentType.HEAT_PUMP,
				ComponentType.HEATING_ROD) <= exportHyst + getPowerExportMax()) {
			heatings.startFirst(ComponentType.HEAT_PUMP, ComponentType.HEATING_ROD);
			logger.debug("Starting electrical heating due to power boundary infringement: {}", power);

		} else {
			set(value);
		}
	}

	private class PowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			logger.trace("Received power value: {}W", value);
			
			Value power = new DoubleValue(value.doubleValue()*powerScale, value.getEpochMillis());
			if (power.doubleValue() != powerValue.doubleValue()) {
				// Only update PID control if the power value changed.
				// Measurements will most probably always differ and only values read via a
				// SCADA
				// system which are no new measurements can be expected to be exactly the same.
				powerValue = power;
				onPowerChanged(power);
			}
		}
	}

	private class PowerLimitListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			logger.trace("Received power limit value: {}W", value);

			powerLimitValue = value;
			onPowerChanged(powerValue);
		}
	}

}
