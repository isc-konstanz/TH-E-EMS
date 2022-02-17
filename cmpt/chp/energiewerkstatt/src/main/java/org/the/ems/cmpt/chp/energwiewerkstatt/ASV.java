package org.the.ems.cmpt.chp.energwiewerkstatt;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
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

	@Configuration
	private Channel setpointPower;

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

}
