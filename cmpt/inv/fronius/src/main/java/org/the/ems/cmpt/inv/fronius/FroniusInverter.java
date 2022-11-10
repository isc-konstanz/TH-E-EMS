package org.the.ems.cmpt.inv.fronius;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.inv.Inverter;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.InvalidValueException;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;


@Component(
	scope = ServiceScope.BUNDLE,
	service = InverterService.class,
	configurationPid = InverterService.PID+".fronius",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class FroniusInverter extends Inverter<FroniusBattery> {
	private static final Logger logger = LoggerFactory.getLogger(FroniusInverter.class);

	@Configuration(value={INPUT_POWER_VALUE,DC_POWER_VALUE})
	private Channel inputPower;

	@Configuration(value={ACTIVE_POWER_VALUE,AC_POWER_VALUE})
	private Channel activePower;

	@Override
	public Value getInputPower() throws InvalidValueException {
		return inputPower.getLatestValue();
	}

	@Override
	public Value getActivePower() throws InvalidValueException {
		return activePower.getLatestValue();
	}

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		inputPower.registerValueListener(new ValueListener() {
			@Override
			public void onValueReceived(Value value) {
				try {
					if (Double.isNaN(value.doubleValue()) && 
							Double.isFinite(getActivePower().doubleValue())) {
						inputPower.setLatestValue(new DoubleValue(0., value.getEpochMillis()));
					}
				} catch (InvalidValueException e) {
					logger.warn("Error retrieving active power: {}", e.getMessage());
				}
			}
		});
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
	}

}
