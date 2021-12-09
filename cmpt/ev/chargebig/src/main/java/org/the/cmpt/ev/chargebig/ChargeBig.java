package org.the.cmpt.ev.chargebig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.ev.ElectricVehicle;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.ElectricVehicleService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Error;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = ElectricVehicleService.class,
	configurationPid = ElectricVehicleService.PID+".chargebig",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ChargeBig extends ElectricVehicle implements ValueListener {
	private static final Logger logger = LoggerFactory.getLogger(ChargeBig.class);

	private static final String CHARGE_POWER_L1_VALUE = "charge_power_l1";
	private static final String CHARGE_POWER_L2_VALUE = "charge_power_l2";
	private static final String CHARGE_POWER_L3_VALUE = "charge_power_l3";

	private static final String CHARGE_CURRENT_L1_VALUE = "charge_current_l1";
	private static final String CHARGE_CURRENT_L2_VALUE = "charge_current_l2";
	private static final String CHARGE_CURRENT_L3_VALUE = "charge_current_l3";

	protected static final double PHASE_CURRENT_MIN = 0;
	protected static final double PHASE_CURRENT_MAX = 200;
	protected static final double PHASE_VOLTAGE = 230;
	protected static final double PHASE_COUNT = 3;

	@Configuration(mandatory = false, scale=1000)
	private double setpointPowerMax = PHASE_CURRENT_MAX*PHASE_VOLTAGE*PHASE_COUNT;

	@Configuration
	private Channel setpointPower;

	@Configuration(mandatory = false)
	private Channel setpointCurrentMax;

	@Configuration
	private Channel setpointCurrent;

	@Configuration
	private Channel setpointEnabled;

	@Configuration(mandatory = false)
	private Channel gridPower;

	@Configuration(mandatory = false, scale=1000)
	private double gridPowerMax = 0;

	@Configuration
	private Channel chargePower;

	@Configuration
	private Channel chargeCurrent;

	private List<ChargePoint> chargePoints;

	@Override
	public Value getChargePower() throws ComponentException, InvalidValueException {
		return chargePower.getLatestValue();
	}

	@Configuration(value=CHARGE_POWER_L1_VALUE)
	public Value getChargePowerL1() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_POWER_L1_VALUE);
	}

	@Configuration(value=CHARGE_POWER_L2_VALUE)
	public Value getChargePowerL2() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_POWER_L2_VALUE);
	}

	@Configuration(value=CHARGE_POWER_L3_VALUE)
	public Value getChargePowerL3() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_POWER_L3_VALUE);
	}

	public Value getChargeCurrent() throws ComponentException, InvalidValueException {
		return chargeCurrent.getLatestValue();
	}

	@Configuration(value=CHARGE_CURRENT_L1_VALUE)
	public Value getChargeCurrentL1() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_CURRENT_L1_VALUE);
	}

	@Configuration(value=CHARGE_CURRENT_L2_VALUE)
	public Value getChargeCurrentL2() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_CURRENT_L2_VALUE);
	}

	@Configuration(value=CHARGE_CURRENT_L3_VALUE)
	public Value getChargeCurrentL3() throws ComponentException, InvalidValueException {
		return getConfiguredValue(CHARGE_CURRENT_L3_VALUE);
	}

	public double getSetpointPowerMaximum() {
		return getSetpointCurrentMaximum()*PHASE_VOLTAGE*PHASE_COUNT;
	}

	public Value getSetpointPower() throws ComponentException, InvalidValueException {
		return setpointPower.getLatestValue();
	}

	private Value getSetpointPower(Value currentValue) throws ComponentException {
		if (currentValue == null) {
			throw new ComponentException("Unable to retrieve setpoint power");
		}
		double current = currentValue.doubleValue();
//		double voltage = getChargePower().doubleValue()
//						/ getChargeCurrent().doubleValue();
		
		double power = current*PHASE_VOLTAGE*PHASE_COUNT;
		
		return new DoubleValue(power, currentValue.getEpochMillis());
	}

	public double getSetpointCurrentMaximum() {
		if (setpointCurrentMax != null) {
			try {
				return setpointCurrentMax.getLatestValue().doubleValue();
				
			} catch (InvalidValueException e) {
				logger.debug("Error retrieving maximum setpoint current: {}", e.getMessage());
			}
		}
		return setpointPowerMax/PHASE_VOLTAGE/PHASE_COUNT;
	}

	private Value getSetpointCurrent(Value powerValue) throws ComponentException {
		if (powerValue == null) {
			throw new ComponentException("Unable to retrieve setpoint current");
		}
		double power = powerValue.doubleValue();
//		double voltage = getChargePower().doubleValue()
//						/ getChargeCurrent().doubleValue();
		
		double current = power/PHASE_VOLTAGE/PHASE_COUNT;
		
		if (current < PHASE_CURRENT_MIN) {
			current = PHASE_CURRENT_MIN;
		}
		if (current > PHASE_CURRENT_MAX) {
			current = PHASE_CURRENT_MAX;
		}
		return new DoubleValue(current, powerValue.getEpochMillis());
	}

	@Override
	protected void onConfigure(Configurations configs) throws ConfigurationException {
		super.onConfigure(configs);
		this.onConfigureChargePoints(configs);
		if (setpointCurrentMax != null) {
			setpointCurrentMax.setLatestValue(new DoubleValue(setpointPowerMax/PHASE_VOLTAGE/PHASE_COUNT));
		}
	}

	private void onConfigureChargePoints(Configurations configs) throws ConfigurationException {
		chargePoints = ChargePoint.newCollection(getContentManagement(), configs);
	}

	@Override
	protected void onActivate(Configurations configs) throws ComponentException {
		//super.onActivate(configs);
		// Do not call super activation to keep run state DEFAULT
		setpointEnabled.registerValueListener(this);
		if (gridPower != null) {
			gridPower.registerValueListener(new PowerListener());
		}
	}

	@Override
	protected void onDeactivate() throws ComponentException {
		//super.onDeactivate();
		setpointEnabled.deregisterValueListeners();
		if (gridPower != null) {
			gridPower.deregisterValueListeners();
		}
	}

	@Override
	protected void onInterrupt() {
		updateChargePower();
		updateChargeCurrent();
		updateSetpointPower();
	}

	@Override
	protected void onSet(WriteContainer container, Value setpointPower) throws ComponentException {
		container.add(setpointCurrent, getSetpointCurrent(setpointPower));
	}

//	@Override
//	public void onValueChanged(Value value) {
//		if (!value.booleanValue()) {
//			enable();
//		}
//	}

	@Override
	public void onError(Error error) {
		enable();
	}

	private void enable() {
		logger.info("Enabling chargeBIG setpoint");
		setpointEnabled.write(new BooleanValue(true));
		setpointCurrent.write(new DoubleValue(getSetpointCurrentMaximum()));
	}

	@SuppressWarnings("serial")
	private void updateChargeCurrent() {
		try {
			List<Value> chargeCurrents = new ArrayList<Value>() {
				{
					add(getChargeCurrentL1());
					add(getChargeCurrentL2());
					add(getChargeCurrentL3());
				}
			};
			if (!chargeCurrents.stream().anyMatch(Objects::isNull)) {
				chargeCurrent.setLatestValue(new DoubleValue(chargeCurrents.stream()
						.mapToDouble(p -> p.doubleValue())
						.sum()));
			}
		} catch (InvalidValueException e) {
			switch (e.getSeverity()) {
			case ERROR:
				logger.warn("Failed to retrieve value for channel {}: {}", e.getSource().getId(), e.getMessage());
			default:
				break;
			}
		} catch (ComponentException e) {
			logger.warn("Failed to update charge current value: {}", e.getMessage());
		}
	}

	@SuppressWarnings("serial")
	private void updateChargePower() {
		try {
			List<Value> chargePowers = new ArrayList<Value>() {
				{
					add(getChargePowerL1());
					add(getChargePowerL2());
					add(getChargePowerL3());
				}
			};
			if (!chargePowers.stream().anyMatch(Objects::isNull)) {
				chargePower.setLatestValue(new DoubleValue(chargePowers.stream()
						.mapToDouble(p -> p.doubleValue())
						.sum()));
			}
		} catch (InvalidValueException e) {
			switch (e.getSeverity()) {
			case ERROR:
				logger.warn("Failed to retrieve value for channel {}: {}", e.getSource().getId(), e.getMessage());
			default:
				break;
			}
		} catch (ComponentException e) {
			logger.warn("Failed to update charge power value: {}", e.getMessage());
		}
	}

	private void updateSetpointPower() {
		try {
			Value setpointCurrentValue = setpointCurrent.getLatestValue();
			Value setpointPowerValue = getSetpointPower(setpointCurrentValue);
			setpointPower.setLatestValue(setpointPowerValue);
			
		} catch (InvalidValueException e) {
			switch (e.getSeverity()) {
			case ERROR:
				logger.warn("Failed to retrieve value for channel {}: {}", e.getSource().getId(), e.getMessage());
			default:
				break;
			}
		} catch (ComponentException e) {
			logger.warn("Failed to update setpoint power value: {}", e.getMessage());
		}
	}

	private class PowerListener implements ValueListener {

		@Override
		public void onValueReceived(Value powerValue) {
			long timestamp = powerValue.getEpochMillis();
			//long timestamp = System.currentTimeMillis();
			
			double gridValue = powerValue.doubleValue();
			try {
				if (chargePoints.stream().filter(Objects::nonNull).anyMatch(c -> c.isCharging())) {
					// FIXME: Think of a way to avoid oscillation of setpoint values
				}
	            double setpointPowerMax = getSetpointPowerMaximum();
				double setpointPowerError = gridPowerMax - gridValue;
				if (setpointPowerError > setpointPowerMax) {
					setpointPowerError = setpointPowerMax;
				}
				if (setpointPowerError < 0) {
					setpointPowerError = 0;
				}
				
				Value setpointPowerValue = new DoubleValue(setpointPowerError, timestamp);
				boolean setpointChanged = setpointPowerError != getSetpointPower().doubleValue();
				setpointPower.setLatestValue(setpointPowerValue);
				if (setpointChanged) {
					set(setpointPowerValue);
				}
			} catch (InvalidValueException e) {
				switch (e.getSeverity()) {
				case ERROR:
					logger.warn("Error retrieving value for channel {}: {}", e.getSource().getId(), e.getMessage());
				default:
					break;
				}
			} catch (EnergyManagementException e) {
				logger.warn("Error updating setpoint value: {}", e.getMessage());
			}
		}

	}

}
