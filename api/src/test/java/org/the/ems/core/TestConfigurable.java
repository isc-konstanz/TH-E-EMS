/* 
 * Copyright 2016-2021 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/th-e-ems
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.the.ems.core;

import java.util.List;

import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.ConfigurationException;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.Channel;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.ValueType;

public class TestConfigurable extends Configurable {

	@Configuration
	private String test;

	@Configuration
	private String testString;

	@Configuration
	private boolean testBoolean;

	@Configuration(scale=10)
	private float testFloat;

	@Configuration(value="test_string")
	private int testInteger;

	@Configuration(value="test_enum")
	private ValueType testValueType;

	@Configuration(value="test_optional", section="Test", mandatory=false)
	private String testOptional = "";

	@Configuration(value="test", section="Test")
	private String testSection;

	@Configuration(value={"test1", "test"})
	private String testSelection;

	@Configuration(value="test*")
	private List<String> testList;

	@Configuration(value={"test", "test_channel1"})
	private ChannelCollection testCollection;

	@Configuration(value="test_channel*")
	private ChannelCollection testWildcard;

	private ConfigurableContext context;

	public TestConfigurable(Configurations configs) throws ConfigurationException {
		context = new ConfigurableContext(this, new TestManager());
		configure(configs);
	}

	public String getTest() {
		return test;
	}

	public String getTestString() {
		return testString;
	}

	public int getTestInteger() {
		return testInteger;
	}

	public float getTestFloat() {
		return testFloat;
	}

	public boolean getTestBoolean() {
		return testBoolean;
	}

	public ValueType getTestValueType() {
		return testValueType;
	}

	public String getTestOptional() {
		return testOptional;
	}

	public String getTestSection() {
		return testSection;
	}

	public String getTestSelection() {
		return testSelection;
	}

	public Channel getTestCollection(String key) {
		return testCollection.get(key);
	}

	public Channel getTestWildcard(String key) {
		return testWildcard.get(key);
	}

	@Override
	protected ConfigurableContext getContext() {
		return context;
	}

}
