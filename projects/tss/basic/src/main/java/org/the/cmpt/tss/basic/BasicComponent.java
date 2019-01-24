package org.the.cmpt.tss.basic;

import org.osgi.service.component.annotations.Component;
import org.the.ems.core.ComponentException;
import org.the.ems.core.ComponentWriteContainer;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.ThermalStorageService;
import org.the.ems.core.cmpt.GenericComponent;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Value;

@Component
public class BasicComponent extends GenericComponent implements ThermalStorageService {
	private final static String ID = "Basic";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeactivate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpdate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onMaintenance(boolean enabled) throws EnergyManagementException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSet(ComponentWriteContainer container, Value value) throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Value getEnergy() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value getTemperature() throws ComponentException {
		// TODO Auto-generated method stub
		return null;
	}

}
