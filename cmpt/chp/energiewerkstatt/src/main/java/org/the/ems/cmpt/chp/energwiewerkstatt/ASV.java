package org.the.ems.cmpt.chp.energwiewerkstatt;

import java.text.MessageFormat;

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
import org.the.ems.core.settings.StartSettings;
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

	private static final double SETPOINT_POWER_TOLERANCE = 100d;
	private static final int SETPOINT_MONITORING_INTERVAL = 5;

	@Configuration()
	private Channel enable;

	@Configuration(value=THERMAL_ENERGY_VALUE, mandatory=false)
	private Channel thermalEnergy;

	@Configuration(value=THERMAL_POWER_VALUE, mandatory=false)
	private Channel thermalPower;

	@Configuration(value=ELECTRICAL_ENERGY_VALUE, mandatory=false)
	private Channel electricalEnergy;

	@Configuration(value=ELECTRICAL_POWER_VALUE)
	private Channel electricalPower;
	private Double electricalPowerValue = Double.NaN;

	@Configuration
	private Channel setpointPower;
	private DoubleValue setpointPowerValue = DoubleValue.emptyValue();

	protected void setSetpointPowerValue(Value value) throws ComponentException {
		if (value.doubleValue() > getMaxPower() || 
				(value.doubleValue() < getMinPower() && value.doubleValue() != 0)) {
			throw new ComponentException(MessageFormat.format("Invalid power value: {0}", value));
		}
		if (value instanceof DoubleValue) {
			setpointPowerValue = (DoubleValue) value;
		}
		else {
			setpointPowerValue = DoubleValue.copy(value);
		}
	}

	protected void setSetpointPowerValue(double value, long timestamp) throws ComponentException {
		this.setSetpointPowerValue(new DoubleValue(value, timestamp));
	}

	protected int getEnabledState() throws InvalidValueException {
		return enable.getLatestValue().intValue();
	}

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
		electricalPower.registerValueListener(new ElectricalPowerListener());
		electricalPower.registerValueListener(new ThermalPowerListener(thermalEnergy));
	}

	@Override
	protected void onSet(WriteContainer container, Value value) throws ComponentException {
		synchronized (setpointPowerValue) {
			if (value.getEpochMillis() - System.currentTimeMillis() > 100 ||
					(setpointPowerValue.isNaN() || 
					setpointPowerValue.doubleValue() != value.doubleValue())) {
				setSetpointPowerValue(value);
				if (logger.isDebugEnabled()) {
					logger.debug("Updating Energiewerkstatt ASV power setpoint to {} kW",
							Math.round(value.intValue()/1000));
				}
				container.add(setpointPower, value);
			}
		}
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		if (logger.isDebugEnabled()) {
			logger.debug("Starting Energiewerkstatt ASV with power setpoint of {} kW",
					Math.round(settings.getValue().intValue()/1000));
		}
		synchronized (setpointPowerValue) {
			setSetpointPowerValue(settings.getValue());
			container.addInteger(enable, 1, settings.getEpochMillis());
			container.add(setpointPower, setpointPowerValue);
		}
	}

	@Override
	public boolean isRunning() throws ComponentException {
		double minimum = getMinPower();
		if (minimum > 0) {
			Double power = electricalPowerValue;
			try {
				power = getElectricalPower().doubleValue();
				
			} catch(ComponentException | InvalidValueException e) {
				logger.debug("Error while checking run state: {}", e.getMessage());
			}
			if (!power.isNaN()) {
				return Math.abs(power) >= minimum;
			}
		}
		return super.isRunning();
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		logger.debug("Stopping Energiewerkstatt ASV");

		synchronized (setpointPowerValue) {
			setSetpointPowerValue(0, settings.getEpochMillis());
			container.addInteger(enable, 0, settings.getEpochMillis());
			container.add(setpointPower, setpointPowerValue);
		}
	}

	@Override
	public boolean isStandby() throws ComponentException {
		Double power = electricalPowerValue;
		try {
			power = getElectricalPower().doubleValue();
			
		} catch(ComponentException | InvalidValueException e) {
			logger.debug("Error while checking standby state: {}", e.getMessage());
		}
		if (!power.isNaN()) {
			return power == 0.0;
		}
		return super.isStandby();
	}

	@Override
	protected void onInterrupt() {
		try {
			synchronized (setpointPowerValue) {
				Value powerValue = getElectricalPower();
				long interval = (System.currentTimeMillis() - setpointPowerValue.getEpochMillis())/1000;
				if (interval % SETPOINT_MONITORING_INTERVAL == 0 && !setpointPowerValue.isNaN() &&
						Math.abs(powerValue.doubleValue() - setpointPowerValue.doubleValue()) > SETPOINT_POWER_TOLERANCE) {
					try {
						if (setpointPowerValue.doubleValue() == 0) {
							stop();
						}
						else if (powerValue.doubleValue() == 0) {
							StartSettings settings = new ValueSettings(setpointPowerValue);
							start(settings);
						}
						else {
							set(setpointPowerValue);
						}
					} catch (EnergyManagementException e) {
						logger.warn("Error updating setpoint power value: {}", e.getMessage());
					}
				}
			}
		} catch (ComponentException | InvalidValueException e) {
			logger.debug("Error while checking setpoint power value: {}", e.getMessage());
			
		}
	}

	private class ElectricalPowerListener extends PowerListener {

		@Override
		public void onValueReceived(Value power) {
			super.onValueReceived(power);
			double powerValue = power.doubleValue();
			try {
				switch (getState()) {
				case STARTING:
				case RUNNING:
					if (powerValue == 0) { //&& (electricalPowerValue.isNaN() || electricalPowerValue >= getMinPower())) {
						ValueSettings stopSettings = ValueSettings.ofBoolean(false, System.currentTimeMillis());
                        stopSettings.setEnforced(true);
						stop();
					}
					break;
				case STANDBY:
				case STOPPING:
					if (powerValue >= getMinPower()) { //&& (electricalPowerValue.isNaN() || electricalPowerValue == 0)) {
						onRunning();
						setState(RunState.RUNNING);
					}
					break;
				default:
					break;
				}
			} catch (EnergyManagementException e) {
				logger.warn("Error synchronizing run state change: {}", e.getMessage());
			}
			electricalPowerValue = powerValue;
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
