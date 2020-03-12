package org.the.cmpt.chp.serenergy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.cmpt.chp.serenergy.data.Request;
import org.the.cmpt.chp.serenergy.data.State;
import org.the.ems.cmpt.chp.Cogenerator;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.CogeneratorService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component(
	scope = ServiceScope.BUNDLE,
	service = CogeneratorService.class,
	configurationPid = CogeneratorService.PID+".serenergy",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class Serenergy extends Cogenerator {

	@Configuration(mandatory = false)
	protected int enableDelay = 2500;

	@Configuration
	protected Channel enable;

	@Configuration
	protected Channel start;

	@Configuration
	protected Channel stop;

	@Configuration
	protected Channel status;

	@Configuration
	private Channel stackLimit;

	@Configuration
	private double stackTempMax;

	@Configuration
	private double stackTempMin;

	@Configuration
	private ChannelListener stackTemp;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);
		
		stackTemp.registerValueListener(new StackTempListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		stackTemp.deregister();
	}

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		long time = value.getTime();
		
		container.add(enable, Request.ENABLE.encode(time));
		container.add(start, Request.START.encode(time+enableDelay));
		// TODO: set stackLimit
	}

	@Override
	protected void onStop(WriteContainer container, long time) throws ComponentException {
		container.add(stop, Request.STOP.encode(time));
		// TODO: reset stackLimit
	}

	@Override
	protected void onSet(WriteContainer container, Value value) throws ComponentException {
		// TODO: set stackLimit
	}

	@Override
	protected void onStateChanged(Value value) throws EnergyManagementException {
		// The parent implementation would start the circulation pump here.
		// This is not needed for the Serenergy fuel cell, as the stack needs 
		// to be on operating temperature first.
	}

	private class StackTempListener implements ValueListener {

		@Override
		public void onValueReceived(Value temp) {
			if (!isMaintenance()) {
				if (temp.doubleValue() > stackTempMax) {
					circulationPump.start();
				}
				else if (temp.doubleValue() < stackTempMin) {
					if (circulationPump.hasRunMinimum()) {
						circulationPump.stop();
					}
					if (State.decode(state.getLatestValue()) == State.STANDBY) {
						enable.write(Request.DISABLE.encode());
					}
				}
			}
		}
	}

}
