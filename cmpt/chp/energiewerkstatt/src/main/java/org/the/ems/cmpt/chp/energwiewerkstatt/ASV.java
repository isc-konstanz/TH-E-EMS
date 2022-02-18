package org.the.ems.cmpt.chp.energwiewerkstatt;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.cmpt.util.PowerListener;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.RunState;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.core.settings.ValueSettings;

@Component(
	scope = ServiceScope.BUNDLE,
	service = CogeneratorService.class,
	configurationPid = CogeneratorService.PID+".energiewerkstatt",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ASV extends Cogenerator {
	private static final Logger logger = LoggerFactory.getLogger(ASV.class);

	@Configuration(value=THERMAL_ENERGY_VALUE, mandatory=false)
	private Channel thermalEnergy;

	@Configuration(value=THERMAL_POWER_VALUE, mandatory=false)
	private Channel thermalPower;

	@Configuration(value=ELECTRICAL_ENERGY_VALUE, mandatory=false)
	private Channel electricalEnergy;

	@Configuration(value=ELECTRICAL_POWER_VALUE)
	private Channel electricalPower;

	@Configuration
	private Channel setpointPower;

	@Override
	public Value getElectricalEnergy() throws ComponentException, InvalidValueException {
		return electricalEnergy.getLatestValue();
	}

	@Override
	public Value getElectricalPower() throws ComponentException, InvalidValueException {
		return electricalPower.getLatestValue();
	}

	@Override
	public Value getThermalEnergy() throws ComponentException, InvalidValueException {
		return thermalEnergy.getLatestValue();
	}

	@Override
	public Value getThermalPower() throws ComponentException, InvalidValueException {
		return thermalPower.getLatestValue();
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		electricalPower.registerValueListener(new ElectricalPowerListener(electricalEnergy));
		electricalPower.registerValueListener(new ThermalPowerListener(thermalEnergy));
	}

	@Override
	protected void onSet(WriteContainer container, Value value) throws ComponentException {
		container.add(setpointPower, value);
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		container.add(setpointPower, settings.getValue());
	}

	@Override
	public boolean isRunning() throws ComponentException {
		double minimum = getMinPower();
		if (minimum > 0) {
			try {
				return Math.abs(getElectricalPower().doubleValue()) >= minimum;
				
			} catch(ComponentException | InvalidValueException e) {
				logger.debug("Error while checking run state: {}", e.getMessage());
			}
		}
		return super.isRunning();
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		container.add(setpointPower, new DoubleValue(0, settings.getEpochMillis()));
	}

	@Override
	public boolean isStandby() throws ComponentException {
		try {
			return getElectricalPower().doubleValue() == 0.0;
			
		} catch(ComponentException | InvalidValueException e) {
			logger.debug("Error while checking standby state: {}", e.getMessage());
		}
		return super.isStandby();
	}

	private class ElectricalPowerListener extends PowerListener {

		private Double powerLast = null;

		public ElectricalPowerListener(Channel energy) {
			super(energy);
		}

		@Override
		public void onValueChanged(Value power) {
			try {
				double powerValue = power.doubleValue();
				if (powerLast != null) {
					switch (getState()) {
					case STARTING:
					case RUNNING:
						if (powerValue == 0 && powerLast >= getMinPower()) {
							stop();
						}
						break;
					case STANDBY:
					case STOPPING:
						if (powerLast == 0 && powerValue >= getMinPower()) {
							onRunning();
							setState(RunState.RUNNING);
						}
						break;
					default:
						break;
					
					}
				}
				powerLast = powerValue;
				
			} catch (EnergyManagementException e) {
				logger.warn("Error synchronizing run state change: {}", e.getMessage());
			}
		}
	}

	private class ThermalPowerListener extends PowerListener {

		public ThermalPowerListener(Channel energy) {
			super(energy);
		}

		@Override
		public void onValueReceived(Value electricalPower) {
			long timestamp = electricalPower.getEpochMillis();

			// Received electrical power
			// Calculate estimated primary and following thermal power
			double primaryPowerValue = electricalPower.doubleValue()/getElectricalEfficiency();
			Value thermalPowerValue = new DoubleValue(primaryPowerValue*getThermalEfficiency(), timestamp);
			if (thermalPower != null) {
				thermalPower.setLatestValue(thermalPowerValue);
			}
			this.onPowerReceived(thermalPowerValue);
		}
	}

}
