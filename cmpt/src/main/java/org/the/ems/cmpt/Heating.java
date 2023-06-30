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

import java.text.MessageFormat;
import java.time.Instant;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.circ.Circulation;
import org.the.ems.cmpt.circ.CirculationPump;
import org.the.ems.core.ComponentContext;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentService;
import org.the.ems.core.ComponentType;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.EnergyManagementService;
import org.the.ems.core.HeatingService;
import org.the.ems.core.HeatingType;
import org.the.ems.core.Season;
import org.the.ems.core.cmpt.ThermalEnergyStorageService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.schedule.Schedule;
import org.the.ems.core.settings.HeatingSettings;
import org.the.ems.core.settings.StartSettings;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.core.settings.ValueSettings;

public abstract class Heating extends Runnable implements HeatingService {
	private final static Logger logger = LoggerFactory.getLogger(Heating.class);

	protected static final String ELECTRICAL_ENERGY_VALUE = "el_energy";
	protected static final String ELECTRICAL_POWER_VALUE = "el_power";

	protected static final String THERMAL_ENERGY_VALUE = "th_energy";
	protected static final String THERMAL_POWER_VALUE = "th_power";

	protected ThermalEnergyStorageService storage;

	@Configuration(scale=1000, mandatory=false)
	private double powerMax = Double.NaN;

	@Configuration(scale=1000)
	private double powerMin;

	protected final Circulation circulation;
	protected final CirculationPump circulationPump;

	protected Heating() {
		super();
		circulation = new Circulation();
		circulationPump = new CirculationPump(circulation);
	}

	@Override
	public Season getSeason() throws ComponentException, InvalidValueException {
		return Season.valueOf(Instant.now());
	}

	@Override
	public ThermalEnergyStorageService getEnergyStorage() throws ComponentException {
		if (storage == null) {
			throw new ComponentException("Thermal energy storage unavailable");
		}
		return storage;
	}

	protected void bindEnergyStorage(ComponentContext context, Configurations configs) 
			throws ComponentException {
		EnergyManagementService manager = context.getEnergyManager();
		for (ComponentService component : manager.getComponents(ComponentType.THERMAL_ENERGY_STORAGE)) {
			bindEnergyStorage((ThermalEnergyStorageService) component);
		}
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void bindEnergyStorage(ThermalEnergyStorageService storageService) throws ComponentException {
		if (storageService instanceof HeatStorage) {
			HeatStorage heatStorage = (HeatStorage) storageService;
			if (heatStorage.bindHeating(this)) {
				this.storage = storageService;
			}
		}
		else {
			this.storage = storageService;
		}
	}

	protected void unbindEnergyStorage(ThermalEnergyStorageService storageService) {
		if (storageService instanceof HeatStorage) {
			HeatStorage heatStorage = (HeatStorage) storageService;
			heatStorage.unbindHeating(this);
		}
		this.storage = null;
	}

	@Configuration(value=ELECTRICAL_ENERGY_VALUE, mandatory=false)
	public Value getElectricalEnergy() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ELECTRICAL_ENERGY_VALUE).getLatestValue();
	}

	public Value getElectricalEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ELECTRICAL_ENERGY_VALUE).getLatestValue(listener);
	}

	public void registerElectricalEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(ELECTRICAL_ENERGY_VALUE).registerValueListener(listener);
	}

	public void deregisterElectricalEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(ELECTRICAL_ENERGY_VALUE).deregisterValueListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Configuration(value=THERMAL_ENERGY_VALUE, mandatory=false)
	public Value getThermalEnergy() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(THERMAL_ENERGY_VALUE).getLatestValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Value getThermalEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(THERMAL_ENERGY_VALUE).getLatestValue(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerThermalEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(THERMAL_ENERGY_VALUE).registerValueListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deregisterThermalEnergyListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(THERMAL_ENERGY_VALUE).deregisterValueListener(listener);
	}

	@Configuration(value=ELECTRICAL_POWER_VALUE, mandatory=false)
	public Value getElectricalPower() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ELECTRICAL_POWER_VALUE).getLatestValue();
	}

	public Value getElectricalPower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(ELECTRICAL_POWER_VALUE).getLatestValue(listener);
	}

	public void registerElectricalPowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(ELECTRICAL_POWER_VALUE).registerValueListener(listener);
	}

	public void deregisterElectricalPowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(ELECTRICAL_POWER_VALUE).deregisterValueListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Configuration(value=THERMAL_POWER_VALUE, mandatory=false)
	public Value getThermalPower() throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(THERMAL_POWER_VALUE).getLatestValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Value getThermalPower(ValueListener listener) throws ComponentException, InvalidValueException {
		return getContext().getDefaultChannel(THERMAL_POWER_VALUE).getLatestValue(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerThermalPowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(THERMAL_POWER_VALUE).registerValueListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deregisterThermalPowerListener(ValueListener listener) throws ComponentException {
		getContext().getDefaultChannel(THERMAL_POWER_VALUE).deregisterValueListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getStartablePower() throws ComponentException, InvalidValueException {
		if (!isRunning()) {
			return getStartPower();
		}
		return 0.;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getStartPower() throws ComponentException, InvalidValueException {
		return getMinPower();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getStoppablePower() throws ComponentException, InvalidValueException {
		if (isRunning()) {
			return getElectricalPower().doubleValue();
		}
		return 0.;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getStopPower() throws ComponentException, InvalidValueException {
		return 0.;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getMaxPower() {
        if (Double.isNaN(powerMax)) {
            return getMinPower();
        }
        return powerMax;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getMinPower() {
        return powerMin;
    }

	@Override
	protected void onActivate(ComponentContext context, Configurations configs) throws ComponentException {
		super.onActivate(context, configs);
		bindEnergyStorage(context, configs);
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		getContext().registerService(getId().concat("_").concat("circ"), configs, circulation);
		getContext().registerService(getId().concat("_").concat("circ_pump"), configs, circulationPump);
	}

	@Override
	protected void onResume() throws ComponentException {
		circulationPump.resume();
	}

	@Override
	protected void onPause() throws ComponentException {
		circulationPump.pause();
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
	}

	@Override
	void doSchedule(Schedule schedule) throws EnergyManagementException {
		WriteContainer container = new WriteContainer();
		
		long startTimeLast = 0;
		for (int i=0; i<schedule.size(); i++) {
			Value value = schedule.get(i);
			if (value.doubleValue() != 0 && value.doubleValue() > getMaxPower() || value.doubleValue() < getMinPower()) {
				throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
			}
			else if (value.doubleValue() == 0) {
				if (value.getEpochMillis() - startTimeLast < getMinRuntime()) {
					logger.debug("Unable to stop component after interval shorter than {}mins", getMinRuntime()/60000);
					continue;
				}
				onStop(container, value);
			}
			else if (i == 0 || schedule.get(i-1).doubleValue() == 0) {
				startTimeLast = value.getEpochMillis();
				onStart(container, value);
			}
			else if (i == 0 || schedule.get(i-1).doubleValue() != value.doubleValue()) {
				onSet(container, value);
			}
		}
		onSchedule(container, schedule);
		write(container);
	}

	@Override
	void doSet(Value value) throws EnergyManagementException {
		if (value.doubleValue() <= 0 ||
				(value.doubleValue() > getMaxPower() || 
				value.doubleValue() < getMinPower())) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		WriteContainer container = new WriteContainer();
		onSet(container, value);
		write(container);
	}

	@Override
	void doStart(WriteContainer container, StartSettings settings) throws EnergyManagementException {
		if (settings instanceof ValueSettings) {
			doStart(container, (ValueSettings) settings);
		}
		if (settings instanceof HeatingSettings) {
			doStart(container, (HeatingSettings) settings);
		}
		onStart(container, settings);
	}

	void doStart(WriteContainer container, HeatingSettings settings) throws EnergyManagementException {
		onStart(container, settings);
		onStart(container, settings.getType());
	}

	protected void onStart(WriteContainer container, HeatingSettings settings) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onStart(WriteContainer container, HeatingType type) throws ComponentException {
		// Default implementation to be overridden
	}

	@Override
	void doStart(WriteContainer container, ValueSettings settings) throws EnergyManagementException {
		Value value = settings.getValue();
		if (value.doubleValue() <= 0 ||
				value.doubleValue() > getMaxPower() || 
				value.doubleValue() < getMinPower()) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		onStart(container, settings);
		onStart(container, value);
	}

	public boolean isRunning(HeatingType type) throws ComponentException, InvalidValueException {
		// Default implementation to be overridden
		return isRunning();
	}

	@Override
	void doStop(WriteContainer container, StopSettings settings) throws EnergyManagementException {
		if (settings instanceof ValueSettings) {
			doStop(container, (ValueSettings) settings);
		}
		if (settings instanceof HeatingSettings) {
			doStop(container, (HeatingSettings) settings);
		}
		onStop(container, settings);
	}

	void doStop(WriteContainer container, HeatingSettings settings) throws EnergyManagementException {
		onStop(container, settings);
		onStop(container, settings.getType());
	}

	protected void onStop(WriteContainer container, HeatingSettings settings) throws ComponentException {
		// Default implementation to be overridden
	}

	protected void onStop(WriteContainer container, HeatingType type) throws ComponentException {
		// Default implementation to be overridden
	}

	public boolean isStandby(HeatingType type) throws ComponentException, InvalidValueException {
		// Default implementation to be overridden
		return isStandby();
	}

	@Override
	protected void onStateChanged(Value state) throws EnergyManagementException {
		super.onStateChanged(state);
		if (state.booleanValue()) {
			if (circulationPump.isEnabled()) {
				circulationPump.start();
			}
		}
	}

}
