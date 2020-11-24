package org.the.cmpt.inv.kaco;

import org.the.ems.cmpt.ees.ElectricalEnergyStorage;
import org.the.ems.core.ComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;


public class BlueplanetHyBat extends ElectricalEnergyStorage {

	@Configuration
	protected ChannelListener soc;

	@Configuration(mandatory=false)
	private int tricklePower = 0;

	@Override
	public Value getStateOfCharge() throws ComponentException {
		Value socValue = soc.getLatestValue();
		if (socValue == null) {
			throw new ComponentException("Unable to retrieve state of charge");
		}
		return socValue;
	}

	public int getTricklePower() {
		return tricklePower;
	}

	void registerStateofCharge(ValueListener listener) {
		soc.registerValueListener(listener);
	}

	void deregister() {
		soc.deregister();
	}

}