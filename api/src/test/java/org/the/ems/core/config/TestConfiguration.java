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
package org.the.ems.core.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.the.ems.core.data.ValueType;

public class TestConfiguration {

	private Configurations configs;

	@Before
	public void newTestConfiguratios() {
		configs = Configurations.create();
		configs.put("General", "test", "Test");
		configs.put("General", "test_string", "1");
		configs.put("General", "test_channel1", "Channel1");
		configs.put("General", "test_channel2", "Channel2");
		configs.put("General", "test_float", 1.0f);
		configs.put("General", "test_boolean", false);
		configs.put("General", "test_enum", ValueType.BOOLEAN);

		configs.put("Test", "test", "Section");
		configs.put("Test", "disabled", false);

		configs.put("Disabled", "disabled", true);
	}

	@Test
	public void testHandler() throws Exception {
		TestHandler handler = new TestHandler(configs);

		assertEquals(ValueType.BOOLEAN, handler.getTestValueType());
		assertEquals(10.0f, handler.getTestFloat(), 0.0f);
		assertEquals(1, handler.getTestInteger());
		assertEquals("1", handler.getTestString());
		assertEquals("Test", handler.getTest());
		assertEquals("Test", handler.getTestSelection());
		assertEquals("Test", handler.getTestCollection("test").getId());
		assertEquals("Section", handler.getTestSection());
		assertEquals("Channel1", handler.getTestCollection("test_channel1").getId());
		assertEquals("Channel1", handler.getTestWildcard("test_channel1").getId());
		assertEquals("Channel2", handler.getTestWildcard("test_channel2").getId());
	}

	@Test
	public void testDisabled() {
		assertFalse(configs.isEnabled("Disabled"));
		assertFalse(configs.isEnabled("Removed"));
		assertTrue(configs.isEnabled("Test"));
		assertTrue(configs.isEnabled("General"));
	}

	@Test(expected = ConfigurationException.class)
	public void testMandatory() throws Exception {
		new TestMandatory(configs);
	}

}
