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
package org.the.ems.cmpt.ees;

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.MaintenanceException;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;


@org.osgi.service.component.annotations.Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricalEnergyStorageService.class,
	configurationPid = ElectricalEnergyStorageService.PID,
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ElectricalEnergyStorage extends Component implements ElectricalEnergyStorageService {

	protected static final String STATE_VALUE = "soc";
	protected static final String POWER_VALUE = "power";
	protected static final String VOLTAGE_VALUE = "voltage";

	@Configuration
	protected double capacity;

	@Configuration(mandatory=false)
	private double socMax = 100;

	@Configuration(mandatory=false)
	private double socMin = 0;

	@Configuration(mandatory=false)
	private double socHyst = 10;

	private volatile boolean socBoundaryFlag = false;

	private final SoCBoundaryListener socBoundaryListener = new SoCBoundaryListener();

	protected ElectricalEnergyStorage(String section) {
		super(section);
	}

	protected ElectricalEnergyStorage() {
		super();
	}

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	public double getMaxStateOfCharge() {
		return socMax;
	}

	@Override
	public double getMinStateOfCharge() {
		return socMin;
	}

	public double getStateOfChargeHysteresis() {
		return socHyst;
	}

	@Override
	@Configuration(value=STATE_VALUE)
	public Value getStateOfCharge() throws ComponentException, InvalidValueException {
		return getConfiguredValue(STATE_VALUE);
	}

	@Override
	public Value getStateOfCharge(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(STATE_VALUE, listener);
	}

	@Override
	public void registerStateOfChargeListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(STATE_VALUE, listener);
	}

	@Override
	public void deregisterStateOfChargeListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(STATE_VALUE, listener);
	}

	@Override
	public boolean isChargable() throws ComponentException, InvalidValueException {
		return this.isChargable(getStateOfCharge().doubleValue());
	}

	protected boolean isChargable(double soc) {
		double socBoundary = getMaxStateOfCharge();
		if (socBoundaryFlag) {
			socBoundary -= getStateOfChargeHysteresis();
		}
		return soc < socBoundary;
	}

	@Override
	public boolean isDischargable() throws ComponentException, InvalidValueException {
		return this.isDischargable(getStateOfCharge().doubleValue());
	}

	protected boolean isDischargable(double soc) {
		double socBoundary = getMinStateOfCharge();
		if (socBoundaryFlag) {
			socBoundary += getStateOfChargeHysteresis();
		}
		return soc > socBoundary;
	}

	@Override
	@Configuration(value=POWER_VALUE, mandatory=false)
	public Value getPower() throws ComponentException, InvalidValueException {
		return getConfiguredValue(POWER_VALUE);
	}

	@Override
	public Value getPower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(POWER_VALUE, listener);
	}

	@Override
	public void registerPowerListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(POWER_VALUE, listener);
	}

	@Override
	public void deregisterPowerListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(POWER_VALUE, listener);
	}

	@Override
	@Configuration(value=VOLTAGE_VALUE, mandatory=false)
	public Value getVoltage() throws ComponentException, InvalidValueException {
		return getConfiguredValue(VOLTAGE_VALUE);
	}

	@Override
	public Value getVoltage(ValueListener listener) throws ComponentException, InvalidValueException {
		return getConfiguredValue(VOLTAGE_VALUE, listener);
	}

	@Override
	public void registerVoltageListener(ValueListener listener) throws ComponentException {
		registerConfiguredValueListener(VOLTAGE_VALUE, listener);
	}

	@Override
	public void deregisterVoltageListener(ValueListener listener) throws ComponentException {
		deregisterConfiguredValueListener(VOLTAGE_VALUE, listener);
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		this.registerStateOfChargeListener(socBoundaryListener);
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		this.deregisterStateOfChargeListener(socBoundaryListener);
	}

    @Override
    public final void schedule(Schedule schedule)
            throws UnsupportedOperationException, EnergyManagementException {
        
        if (isMaintenance()) {
            throw new MaintenanceException("Unable to schedule battery while in maintenance");
        }
        doSchedule(schedule);
    }

    protected void doSchedule(Schedule schedule) 
            throws UnsupportedOperationException, EnergyManagementException {

        WriteContainer container = new WriteContainer();
        for (Value value : schedule) {
            onSet(container, value);
        }
        onSchedule(container, schedule);
        write(container);
    }

    protected void onSchedule(WriteContainer container, Schedule schedule) 
            throws UnsupportedOperationException, ComponentException {
        // Default implementation to be overridden
    }

    @Override
    public final void set(Value value) 
            throws UnsupportedOperationException, EnergyManagementException {

		if (isMaintenance()) {
			throw new MaintenanceException();
		}
		if (value.doubleValue() > 0 && !isChargable()) {
			throw new ComponentException("Unable to charge battery");
		}
		if (value.doubleValue() < 0 && !isDischargable()) {
			throw new ComponentException("Unable to discharge battery");
		}
        doSet(value);
    }

    protected void doSet(Value value)
            throws UnsupportedOperationException, EnergyManagementException {

        WriteContainer container = new WriteContainer();
        onSet(container, value);
        write(container);
    }

    protected void onSet(WriteContainer container, Value value)
            throws UnsupportedOperationException, ComponentException {
        // Default implementation to be overridden
        throw new UnsupportedOperationException();
    }

	private class SoCBoundaryListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			double state = value.doubleValue();
			if (!isChargable(state) || !isDischargable(state)) {
				socBoundaryFlag = true;
			}
			else if (isChargable(state) && isDischargable(state)) {
				socBoundaryFlag = false;
			}
		}
	}

}
