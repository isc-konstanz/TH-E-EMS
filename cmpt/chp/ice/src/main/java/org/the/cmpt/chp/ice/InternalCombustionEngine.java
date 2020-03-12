package org.the.cmpt.chp.ice;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = CogeneratorService.class,
	configurationPid = CogeneratorService.PID+".ice",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class InternalCombustionEngine extends Cogenerator {

	@Configuration(mandatory = false)
	protected int enableDelay = 5000;

	@Configuration
	protected Channel enable;

	@Configuration(mandatory = false)
	protected Channel valve = null;

	@Configuration(mandatory = false)
	protected int starterTime = 2500;

	@Configuration(mandatory = false)
	protected Channel starter = null;

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		long time = value.getTime();
		
		if (valve != null) {
			container.addBoolean(valve, true, time);
			time += enableDelay;
		}
		container.addBoolean(enable, true, time);
		
		if (starter != null) {
			container.addBoolean(starter, true, time);
			container.addBoolean(starter, false, time+starterTime);
		}
		// TODO: set power level
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		if (valve != null) {
			container.addBoolean(valve, false, time);
			time += enableDelay;
		}
		container.addBoolean(enable, false, time);
		// TODO: reset power level
	}

	@Override
	protected void onSet(WriteContainer container, Value value) throws ComponentException {
		// TODO: set power level
	}

}
