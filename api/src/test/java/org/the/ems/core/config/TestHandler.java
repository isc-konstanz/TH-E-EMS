/*
 * Copyright 2011-18 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.the.ems.core.config;

import org.the.ems.core.ComponentException;
import org.the.ems.core.data.ChannelCollection;
import org.the.ems.core.data.ValueType;

public class TestHandler extends ConfigurationHandler {

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

	@Configuration(value= {"test1","test"})
	private String testSelection;

	@Configuration(value= {"test","test_channel"})
	private ChannelCollection testCollection;

	public TestHandler(Configurations configs) throws ConfigurationException {
		context = new TestManager();
		onConfigure(configs);
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

	public TestChannel getTestCollection(String key) {
		return (TestChannel) testCollection.get(key);
	}

	@Configuration
	public TestChannel getTestChannel() throws ComponentException {
		return (TestChannel) getConfiguredChannel();
	}

}
