package org.the.cmpt.chp.serenergy;

import org.osgi.service.component.annotations.Component;
import org.the.ems.core.CogeneratorService;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.chp.CogeneratorComponent;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;

@Component(service = CogeneratorService.class)
public class Serenergy extends CogeneratorComponent {
	private final static String ID = "Serenergy";

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
	protected void onStart(ComponentWriteContainer container, Value value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onStop(ComponentWriteContainer container, Long time) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	private class StackTempListener implements ValueListener {

		@Override
		public void onValueReceived(Value temp) {
			if (!isMaintenance()) {
				if (temp.doubleValue() > stackTempMax) {
					circulationPump.start();
				}
				else if (temp.doubleValue() < stackTempMin &&
						circulationPump.isRunInterval()) {
					circulationPump.stop();
				}
			}
		}
	}

}
