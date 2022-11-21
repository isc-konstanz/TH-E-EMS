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
	private static final int SETPOINT_MONITORING_INTERVAL = 10;
	private static final int SETPOINT_WRITE_INTERVAL = 900; // 15 Minutes interval

	@Configuration()
	private Channel enable;

	@Configuration(value=THERMAL_ENERGY_VALUE, mandatory=false)
	private Channel thermalEnergy;

	@Configuration(value=THERMAL_POWER_VALUE, mandatory=false)
	private Channel thermalPower;

	@Configuration(value=ELECTRICAL_POWER_VALUE)
	private Channel electricalPower;
	private Value electricalPowerValue = DoubleValue.emptyValue();

	@Configuration(mandatory=false)
	private Channel setpoint;

	@Configuration
	private Channel setpointPower;

	private volatile DoubleValue setpointPowerValue = DoubleValue.emptyValue();

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

	protected Value getSetpointPowerValue() {
		return setpointPowerValue;
	}

	private void addSetpointPercentValue(WriteContainer container) {
		if (setpoint == null) {
			return;
		}
		container.add(setpoint, getSetpointPercentValue());
	}

	protected Value getSetpointPercentValue() {
		return new DoubleValue(
				setpointPowerValue.doubleValue()/getMaxPower()*100., 
				setpointPowerValue.getEpochMillis());
	}

	protected int getEnabledState() throws InvalidValueException {
		return enable.getLatestValue().intValue();
	}

	@Override
	public Value getElectricalPower() throws ComponentException, InvalidValueException {
		return electricalPowerValue;
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
		if (setpointPowerValue.isNaN() || setpointPowerValue.doubleValue() != value.doubleValue()) {
			setSetpointPowerValue(value);
			addSetpointPercentValue(container);
			if (logger.isDebugEnabled()) {
				logger.debug("Updating Energiewerkstatt ASV power setpoint to {} kW",
						Math.round(value.intValue()/1000));
			}
			container.add(setpointPower, getSetpointPowerValue());
		}
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
		if (logger.isDebugEnabled()) {
			logger.debug("Starting Energiewerkstatt ASV with power setpoint of {} kW",
					Math.round(settings.getValue().intValue()/1000));
		}
		setSetpointPowerValue(settings.getValue());
		addSetpointPercentValue(container);
		container.addBoolean(enable, true, settings.getEpochMillis());
		container.add(setpointPower, getSetpointPowerValue());
	}

	@Override
	public boolean isRunning() throws ComponentException, InvalidValueException {
		double minimum = getMinPower();
		if (minimum > 0) {
			double power = electricalPowerValue.doubleValue();
			try {
				power = getElectricalPower().doubleValue();
				
			} catch(ComponentException | InvalidValueException e) {
				logger.debug("Error while checking run state: {}", e.getMessage());
			}
			if (!Double.isNaN(power)) {
				return Math.abs(power) >= minimum;
			}
		}
		return super.isRunning();
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		logger.debug("Stopping Energiewerkstatt ASV");

		setSetpointPowerValue(0, settings.getEpochMillis());
		addSetpointPercentValue(container);
		container.add(setpointPower, getSetpointPowerValue());
		container.addBoolean(enable, false, settings.getEpochMillis());
	}

	@Override
	public boolean isStandby() throws ComponentException, InvalidValueException {
		double power = electricalPowerValue.doubleValue();
		try {
			power = getElectricalPower().doubleValue();
			
		} catch(ComponentException | InvalidValueException e) {
			logger.debug("Error while checking standby state: {}", e.getMessage());
		}
		if (!Double.isNaN(power)) {
			return power == 0.0;
		}
		return super.isStandby();
	}

	@Override
	protected void onInterrupt() {
		try {
			if (!setpointPowerValue.isNaN()) {
				Value powerValue = getElectricalPower();
				long timestamp = System.currentTimeMillis();
				int interval = (int) ((timestamp - setpointPowerValue.getEpochMillis())/1000);
				if (interval % SETPOINT_MONITORING_INTERVAL == 0 && isReady(timestamp) &&
						SETPOINT_POWER_TOLERANCE < Math.abs(powerValue.doubleValue() - setpointPowerValue.doubleValue())) {
					try {
						set(setpointPowerValue);
						
					} catch (EnergyManagementException e) {
						logger.warn("Error updating setpoint power value: {}", e.getMessage());
					}
				}
				else if (interval >= SETPOINT_WRITE_INTERVAL) {
					WriteContainer container = new WriteContainer();
					// Update latest setpoint timestamp
					double setpointPowerValue = this.setpointPowerValue.doubleValue();
					setSetpointPowerValue(new DoubleValue(setpointPowerValue, timestamp));
					addSetpointPercentValue(container);
					container.addDouble(setpointPower, setpointPowerValue, timestamp);
					container.addBoolean(enable, setpointPowerValue > 0., timestamp);
					write(container);
				}
			}
		} catch (EnergyManagementException e) {
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
				case STANDBY:
					if (powerValue >= getMinPower()) {
						onRunning();
						setState(RunState.RUNNING);
					}
					break;
				case STOPPING:
				case RUNNING:
					if (powerValue == 0) {
						try {
							if (setpointPower.getLatestValue().doubleValue() > 0 || 
									enable.getLatestValue().booleanValue()) {
								ValueSettings stopSettings = ValueSettings.ofBoolean(false, System.currentTimeMillis());
		                        stopSettings.setEnforced(true);
								stop(stopSettings);
								break;
							}
						} catch (InvalidValueException e) {
							logger.warn("Error retrieving setpoint values while synchronizing run state: {}", e.getMessage());
						}
						onStandby();
						setState(RunState.STANDBY);
					}
					break;
				default:
					break;
				}
			} catch (EnergyManagementException e) {
				logger.warn("Error synchronizing run state change: {}", e.getMessage());
			}
			electricalPowerValue = power;
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
