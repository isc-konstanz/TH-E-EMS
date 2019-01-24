package org.the.cmpt.inv.kaco.blueplanet;

import org.osgi.service.component.annotations.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.ElectricalStorageService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.InverterService;
import org.the.ems.core.cmpt.inv.InverterComponent;
import org.the.ems.core.cmpt.inv.InverterConfig;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;

@Component
public class BlueplanetComponent extends InverterComponent implements InverterService, ElectricalStorageService {
	private final static String ID = "KACO blueplanet";

	private Channel storage;

	private Channel soc;
	private double socMin;
	private double capacity;

	private double setpointLast = 0;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public org.the.ems.core.Component getType() {
		return org.the.ems.core.Component.INVERTER;
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);
		BlueplanetConfig config = configs.getSection(BlueplanetConfig.class);
		
		storage = manager.getChannel(config.getChargeSetpoint());
		
		soc = manager.getChannel(config.getStateOfCharge());
		socMin = config.getStateMin();
		capacity = config.getCapacity();
	}

	@Override
	public void set(ComponentWriteContainer container, Value value) throws ComponentException {
		if (value.doubleValue() == 0 && setpoint.getChannel().getLatestValue().doubleValue() != 0) {
			container.add(this.storage, InverterConfig.SETPOINT_DEFAULT);
			return;
		}
		double setpoint = consumption.getLatestValue().doubleValue() - value.doubleValue();
		
		if (setpoint == setpointLast) {
			// Do Nothing
			return;
		}
		Value state = soc.getLatestValue();
		if (state != null && state.doubleValue() < socMin) {
			try {
				set(InverterConfig.SETPOINT_DEFAULT);
				
			} catch (EnergyManagementException e) {
			}
			throw new ComponentException("Battery State of Charge below boundaries. Export temporarily disabled.");
		}
		if (setpoint == 0) {
			// The EDCOM software will ignore possible external PV power if set 0
			setpoint = 0.001;
		}
		setpointLast = setpoint;
		
		container.add(this.storage, new DoubleValue(setpoint, value.getTime()));
	}

	@Override
	public Value getEnergy() throws ComponentException {
		Value soc = getStateOfCharge();
		Value value = new DoubleValue(soc.doubleValue()/100*capacity, soc.getTime());
		return value;
	}

	@Override
	public Value getStateOfCharge() throws ComponentException {
		return soc.getLatestValue();
	}

}
