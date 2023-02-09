package org.the.ems.cmpt.ev.flotteladen;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ev.ElectricVehicle;
import org.the.ems.cmpt.util.PowerListener;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.RunState;
import org.the.ems.core.cmpt.ElectricVehicleService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ByteValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.settings.StopSettings;
import org.the.ems.core.settings.ValueSettings;


@Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricVehicleService.class,
	configurationPid = ElectricVehicleService.PID+".flotteladen",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ChargingPoint extends ElectricVehicle implements ValueListener {
	private static final Logger logger = LoggerFactory.getLogger(ChargingPoint.class);

	private static final int DEFAULT_VOLTAGE = 230;
	private static final int DEFAULT_CURRENT_MIN = 6;
	private static final int DEFAULT_CURRENT_MAX = 32;

	private ChargingPointStatus statusValue = ChargingPointStatus.STANDBY;

	@Configuration(mandatory = false)
	private Channel statusCode;

	@Configuration
	private Channel status;

	@Configuration(mandatory = false)
	private Channel energy;

	@Configuration(mandatory = false)
	private Channel power;

	@Configuration(mandatory = false)
	private Channel voltage;

	@Configuration
	private Channel current;

	@Configuration
	private Channel currentMin;

	@Configuration
	private Channel currentMax;

	@Configuration
	private Channel currentSetpoint;

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getMaxPower() {
        return Math.min(getMaxCurrent()*getVoltage()*3, super.getMaxPower());
    }

    public double getMaxCurrent() {
    	try {
			return currentMax.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving maximum current: {}", e.getMessage());
		}
    	return DEFAULT_CURRENT_MAX;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public double getMinPower() {
        return Math.max(getMinCurrent()*getVoltage()*3, super.getMinPower());
    }

    public double getMinCurrent() {
    	try {
			return currentMin.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving minimum current: {}", e.getMessage());
		}
    	return DEFAULT_CURRENT_MIN;
    }

    public double getVoltage() {
    	if (voltage != null) {
        	try {
    			return voltage.getLatestValue().doubleValue();
    			
    		} catch (InvalidValueException e) {
    			logger.debug("Error retrieving voltage: {}", e.getMessage());
    		}
    	}
		return DEFAULT_VOLTAGE;
    }

	@Override
	public Value getChargedEnergy() throws ComponentException, InvalidValueException {
		return energy.getLatestValue();
	}

	@Override
	public Value getChargedEnergy(ValueListener listener) throws ComponentException, InvalidValueException {
		return energy.getLatestValue(listener);
	}

	@Override
	public void registerChargedEnergyListener(ValueListener listener) throws ComponentException {
		energy.registerValueListener(listener);
	}

	@Override
	public void deregisterChargedEnergyListener(ValueListener listener) throws ComponentException {
		energy.deregisterValueListener(listener);
	}

	private Value updateChargePower() throws InvalidValueException {
		return updateChargePower(current.getLatestValue());
	}

	private Value updateChargePower(Value current) {
		double powerDouble;
		if (statusValue != ChargingPointStatus.CHARGING) {
			powerDouble = 0;
		}
		else {
			powerDouble = getVoltage()*current.doubleValue()*3;
		}
		Value powerValue = new DoubleValue(powerDouble, current.getEpochMillis());
		if (power != null) {
			power.setLatestValue(powerValue);
		}
		return powerValue;
	}

	@Override
	public Value getChargePower() throws ComponentException, InvalidValueException {
		return power.getLatestValue();
	}

	@Override
	public Value getChargePower(ValueListener listener) throws ComponentException, InvalidValueException {
		return power.getLatestValue(listener);
	}

	@Override
	public void registerChargePowerListener(ValueListener listener) throws ComponentException {
		power.registerValueListener(listener);
	}

	@Override
	public void deregisterChargePowerListener(ValueListener listener) throws ComponentException {
		power.deregisterValueListener(listener);
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		if (statusCode != null) {
			statusCode.setLatestValue(new ByteValue(statusValue.getCode()));
		}
		status.registerValueListener(this);
		current.registerValueListener(new CurrentListener(energy));
	}

	@Override
	protected void onStart(WriteContainer container, ValueSettings settings) throws ComponentException {
    	Value setpointPower = settings.getValue();
    	double setpointCurrentValue = setpointPower.doubleValue()/getVoltage();
    	
		if (logger.isDebugEnabled()) {
			logger.debug("Starting Flotteladen {} charging with setpoint of {} kW", getId(),
					Math.round(setpointPower.doubleValue()/1000));
		}
		container.add(currentSetpoint, new DoubleValue(setpointCurrentValue, settings.getEpochMillis()));
	}

	@Override
	public boolean isStartable(long timestamp) {
		if (!isConnected()) {
			return false;
		}
		return super.isStartable(timestamp);
	}

	public boolean isConnected() {
		return statusValue == ChargingPointStatus.CONNECTED;
	}

	@Override
	public boolean isRunning() {
		return isCharging();
	}

	@Override
	protected void onSet(WriteContainer container, Value setpointPower) throws ComponentException {
		if (!isCharging()) {
			throw new ComponentException("Unable to update charging setpoint for charging point state: " + statusValue);
		}
		double currentValue = 0;
    	try {
    		currentValue = current.getLatestValue().doubleValue();
			
		} catch (InvalidValueException e) {
			logger.debug("Error retrieving current: {}", e.getMessage());
		}
    	double setpointCurrentValue = setpointPower.doubleValue()/getVoltage()/3;
		if (setpointCurrentValue != currentValue) {
			if (logger.isDebugEnabled()) {
				logger.debug("Updating Flotteladen {} charging setpoint to {} kW", getId(),
						Math.round(setpointPower.doubleValue()/1000));
			}
			container.add(currentSetpoint, new DoubleValue(setpointCurrentValue, setpointPower.getEpochMillis()));
		}
	}

	@Override
	protected void onStop(WriteContainer container, StopSettings settings) throws ComponentException {
		if (logger.isDebugEnabled()) {
			logger.debug("Stopping Flotteladen {} charging", getId());
		}
		container.add(currentSetpoint, new DoubleValue(0, settings.getEpochMillis()));
	}

	@Override
	public boolean isStoppable(long timestamp) {
		if (!isCharging()) {
			return false;
		}
		return super.isStoppable(timestamp);
	}

	@Override
	public boolean isStandby() {
		return !isCharging();
	}

	public boolean isCharging() {
		return statusValue == ChargingPointStatus.CHARGING;
	}

	@Override
	public void onValueReceived(Value status) {
		logger.info("Received status: {}", status.stringValue());
		ChargingPointStatus statusValue = ChargingPointStatus.valueOf(status);
		if (this.statusValue == statusValue) {
			return;
		}
		this.statusValue = statusValue;
		if (statusCode != null) {
			statusCode.setLatestValue(new ByteValue(statusValue.getCode(), status.getEpochMillis()));
		}
		try {
			switch (statusValue) {
			case AVAILABLE:
			case CONNECTED:
			case CHARGING_COMPLETE:
				onStandby();
				setState(RunState.STANDBY);
				updateChargePower();
				break;
			case CHARGING_STARTED_EV:
				setState(RunState.STARTING);
				break;
			case CHARGING:
				onRunning();
				setState(RunState.RUNNING);
				updateChargePower();
				break;
			default:
				break;
			}
		} catch (EnergyManagementException e) {
			logger.warn("Error synchronizing run state change: {}", e.getMessage());
		}
	}

	private class CurrentListener extends PowerListener {

		public CurrentListener(Channel energy) {
			super(energy);
		}

		@Override
		public void onValueReceived(Value current) {
			Value powerValue = updateChargePower(current);
			this.onPowerReceived(powerValue);
		}
	}

}
