package org.the.cmpt.chp.serenergy;

import org.osgi.service.component.annotations.Component;
import org.the.ems.core.CogeneratorService;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.chp.CogeneratorComponent;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;

@Component
public class SerenergyComponent extends CogeneratorComponent implements CogeneratorService {
	private final static String ID = "Serenergy";

	private Double stackTemperatureMax;
	private Double stackTemperatureMin;

	private ChannelListener stackListener;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		SerenergyConfig config = configs.getSection(SerenergyConfig.class);
		
		this.stackTemperatureMax = config.getStackTemperatureMax();
		this.stackTemperatureMin = config.getStackTemperatureMin();
		this.stackListener = registerStackListener(manager.getChannel(config.getStackTemperature()));
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();
		if (stackListener != null) {
			stackListener.deregister();
		}
	}

	private ChannelListener registerStackListener(Channel channel) {
		ChannelListener listener = new ChannelListener(channel) {
			
			@Override
			public void onValueReceived(Value value) {
				onStackTemperatureUpdated(value);
			}
		};
		return listener;
	}

	@Override
	public void start(ComponentWriteContainer container, Value value) throws ComponentException {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop(ComponentWriteContainer container, Long value) throws ComponentException {
		// TODO Auto-generated method stub
	}

	public void onStackTemperatureUpdated(Value temperature) {
		if (!isMaintenance()) {
			if (temperature.doubleValue() > stackTemperatureMax) {
				circulationPump.start();
			}
			else if (temperature.doubleValue() < stackTemperatureMin &&
					circulationPump.isRunInterval()) {
				circulationPump.stop();
			}
		}
	}

}
