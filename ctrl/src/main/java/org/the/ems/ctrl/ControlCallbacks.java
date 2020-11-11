package org.the.ems.ctrl;

import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.data.Value;

public interface ControlCallbacks {

	void onStart(ControlledHeating heating, Value value) throws EnergyManagementException;

	void onStop(ControlledHeating heating) throws EnergyManagementException;

	void onSet(ControlledHeating heating, Value value) throws EnergyManagementException;

	void onSet(ControlledInverter inverter, Value value) throws EnergyManagementException;

}
