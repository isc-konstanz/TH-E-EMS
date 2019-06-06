package org.the.cmpt.inv.kaco.blueplanet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.the.ems.core.ComponentException;
import org.the.ems.core.cmpt.ElectricalEnergyStorageService;
import org.the.ems.core.cmpt.InverterService;
import org.the.ems.core.cmpt.inv.InverterComponent;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.WriteContainer;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

@Component(service = InverterService.class)
public class BlueplanetHybrid extends InverterComponent {
	private final static String ID = "KACO blueplanet hybrid";

	@Reference(service = ElectricalEnergyStorageService.class)
	private BlueplanetBattery battery;

	@Configuration
	protected ChannelListener setpoint;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	protected void onSetpointChanged(WriteContainer container, Value value) throws ComponentException {
		if (battery.getStateOfCharge().doubleValue() < battery.getMinStateOfCharge()) {
			if (setpoint.getLatestValue().doubleValue() != 0) {
				container.add(this.setpoint, new DoubleValue(0));
			}
			return;
		}
		Value setpoint = new DoubleValue(value.doubleValue()*-1, value.getTime());
		container.add(this.setpoint, setpoint);
	}

}
