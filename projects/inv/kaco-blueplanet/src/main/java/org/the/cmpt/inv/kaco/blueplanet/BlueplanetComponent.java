package org.the.cmpt.inv.kaco.blueplanet;

import org.osgi.service.component.annotations.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.ElectricalStorageService;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.InverterService;
import org.the.ems.core.cmpt.inv.InverterComponent;
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
		double setpoint = value.doubleValue()*-1;
		
		Value consumption = this.consumption.getLatestValue();
		if (consumption != null) {
			setpoint += consumption.doubleValue();
		}
		if (setpoint == setpointLast) {
			// Do Nothing
			return;
		}
		
		if (setpoint > setpointMax) {
			setpoint = setpointMax;
		}
		else if (setpoint < setpointMin) {
			setpoint = setpointMin;
		}
		else {
			// The EDCOM software will ignore possible external PV power if set 0
			setpoint = 0.001;
		}
		// TODO: only check SoC if timestamp is now
		Value state = soc.getLatestValue();
		if (setpoint < 0 && state != null && state.doubleValue() < socMin) {
			setpoint = 0;
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
