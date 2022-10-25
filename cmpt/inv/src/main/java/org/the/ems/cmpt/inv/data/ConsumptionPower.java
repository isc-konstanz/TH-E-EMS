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
package org.the.ems.cmpt.inv.data;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.cmpt.inv.Inverter;
import org.the.ems.cmpt.inv.InverterCallbacks;
import org.the.ems.cmpt.inv.data.PowerTypeListener.PowerCallbacks;
import org.the.ems.core.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueList;
import org.the.ems.core.data.ValueListener;

public class ConsumptionPower extends Component implements Channel, ValueListener, PowerCallbacks {

	private static final Logger logger = LoggerFactory.getLogger(ConsumptionPower.class);

	private final static String SECTION = "Consumption";

	private volatile InverterCallbacks callbacks = null;

	@Configuration(value="power_cons", section=Configurations.GENERAL)
	private Channel consumptionPower;

	@Configuration(value="power_active*")
	protected ChannelCollection activePowers;

	@Configuration(value="power_input*")
	protected ChannelCollection inputPowers;

	private final ElectricalEnergyStorage storage;

	protected long timestampLast = -1;

	private volatile boolean running = false;

	public ConsumptionPower(Inverter<?> inverter) {
		super(SECTION);
		this.storage = inverter.getElectricalEnergyStorage();
	}

	@Override
	protected void onActivate() throws ComponentException {
		super.onActivate();
		if (isEnabled()) {
			if (storage.isEnabled()) {
				storage.registerPowerListener(new PowerTypeListener(this, PowerType.EES));
			}
			for (Channel power : inputPowers.values()) {
				power.registerValueListener(new PowerTypeListener(this, PowerType.INPUT));
			}
			for (Channel power : activePowers.values()) {
				power.registerValueListener(new PowerTypeListener(this, PowerType.GRID));
			}
			running = true;
		}
		consumptionPower.registerValueListener(this);
	}

	public void register(InverterCallbacks callbacks) {
		this.callbacks = callbacks;
	}

	public void resume() {
		running = true;
	}

	public void pause() {
		running = false;
	}

	public void deregister() {
		this.callbacks = null;
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		super.onDeactivate();
		this.running = false;
	}

	public Value getLatestValue() throws InvalidValueException {
		return consumptionPower.getLatestValue();
	}

	@Override
	public Value getLatestValue(ValueListener listener) throws InvalidValueException {
		return consumptionPower.getLatestValue(listener);
	}

	@Override
	public void registerValueListener(ValueListener listener) {
		consumptionPower.registerValueListener(listener);
	}

	@Override
	public void deregisterValueListener(ValueListener listener) {
		consumptionPower.deregisterValueListener(listener);
	}

	@Override
	public void deregisterValueListeners() {
		consumptionPower.deregisterValueListeners();
	}

	@Override
	public void setLatestValue(Value value) {
		consumptionPower.setLatestValue(value);
	}

	@Override
	public void write(ValueList values) throws EnergyManagementException {
		consumptionPower.write(values);
	}

	@Override
	public void write(Value value) throws EnergyManagementException {
		consumptionPower.write(value);
	}

	public boolean isRunning() {
		return isEnabled() && running;
	}

	@Override
	public void onPowerValueReceived(PowerType type, Value value) {
        if (!isRunning()) {
            return;
        }
		try {
			long timestampMax = -1;
			double consumption = 0;
			
			for (Channel power : Stream.of(activePowers, inputPowers)
	                .flatMap(x -> x.stream())
	                .collect(Collectors.toList())) {
				
				Value powerValue = power.getLatestValue();
				if (powerValue.getEpochMillis() <= timestampLast) {
					return;
				}
				if (powerValue.getEpochMillis() > timestampMax) {
					timestampMax = powerValue.getEpochMillis();
				}
				consumption += power.getLatestValue().doubleValue();
			}
			if (storage.isEnabled()) {
				consumption -= storage.getPower().doubleValue();
			}
			if (consumption < 0) {
				consumption = 0;
			}
			consumptionPower.setLatestValue(new DoubleValue(consumption, timestampMax));
			timestampLast = timestampMax;
			
		} catch (ComponentException | InvalidValueException e) {
			logger.debug("Unable to calculate power consumption: {}", e.getMessage());
		}
	}

	@Override
	public void onValueReceived(Value value) {
		// Always notify callback of received consumption values, as consumption may be a generic sampled channel
		if (callbacks != null) {
			callbacks.onSetpointUpdate();
		}
	}

}
