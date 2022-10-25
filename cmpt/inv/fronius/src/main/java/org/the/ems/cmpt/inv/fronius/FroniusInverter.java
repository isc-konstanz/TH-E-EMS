package org.the.ems.cmpt.inv.fronius;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.the.ems.cmpt.inv.Inverter;
import org.the.ems.core.cmpt.InverterService;


@Component(
	scope = ServiceScope.BUNDLE,
	service = InverterService.class,
	configurationPid = InverterService.PID+".fronius",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class FroniusInverter extends Inverter<FroniusBattery> {

}
