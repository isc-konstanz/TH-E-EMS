package org.the.cmpt.chp.serenergy;

import org.osgi.service.component.annotations.Component;
import org.the.cmpt.chp.serenergy.data.Request;
import org.the.cmpt.chp.serenergy.data.State;
import org.the.ems.cmpt.chp.CogeneratorComponent;
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

@Component(service = CogeneratorService.class)
public class Serenergy extends CogeneratorComponent {
	private final static String ID = "Serenergy";

	@Configuration
	protected Channel start;

	@Configuration
	protected Channel stop;

	@Configuration
	private Channel stackLimit;

	@Configuration
	private double stackTempMax;

	@Configuration
	private double stackTempMin;

	@Configuration
	private ChannelListener stackTemp;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		
		stackTemp.registerValueListener(new StackTempListener());
	}

	@Override
	public void onDeactivate() throws EnergyManagementException {
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
	protected void onStop(WriteContainer container, Long time) throws ComponentException {
		container.add(stop, Request.STOP.encode(time));
		// TODO: reset stackLimit
	}

	@Override
	protected void onStateChanged(Value value) {
		State state = State.decode(value);
		switch(state) {
		case ON:
			startTimeLast = value.getTime();
			break;
		default:
			break;
		}
	}

	private class StackTempListener implements ValueListener {

		@Override
		public void onValueReceived(Value temp) {
			if (!isMaintenance()) {
				if (temp.doubleValue() > stackTempMax) {
					circulationPump.start();
				}
				else if (temp.doubleValue() < stackTempMin) {
					if (circulationPump.isRunInterval()) {
						circulationPump.stop();
					}
					if (State.decode(state.getLatestValue()) == State.OFF) {
						enable.write(Request.DISABLE.encode());
					}
				}
			}
		}
	}

}
