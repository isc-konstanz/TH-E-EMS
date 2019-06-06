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
		configs = new Configurations();
		configs.add("General", "test", "Test");
		configs.add("General", "test_string", "1");
		configs.add("General", "test_channel", "Channel");
		configs.add("General", "test_float", 1.0f);
		configs.add("General", "test_boolean", false);
		configs.add("General", "test_enum", ValueType.BOOLEAN);

		configs.add("Test", "test", "Test");
		configs.add("Test", "disabled", false);

		configs.add("Disabled", "disabled", true);
	}

	@Test
	public void testHandler() throws Exception {
		TestHandler handler = new TestHandler(configs);

		assertEquals(ValueType.BOOLEAN, handler.getTestValueType());
		assertEquals(10.0f, handler.getTestFloat(), 0.0f);
		assertEquals(1, handler.getTestInteger());
		assertEquals("1", handler.getTestString());
		assertEquals("Test", handler.getTest());
		assertEquals("Test", handler.getTestSection());
		assertEquals("Test", handler.getTestSelection());
		assertEquals("Test", handler.getTestCollection("test").getId());
		assertEquals("Channel", handler.getTestCollection("test_channel").getId());
		assertEquals("Channel", handler.getTestChannel().getId());
	}

	@Test
	public void testDisabled() {
		assertTrue(configs.isDisabled("Disabled"));
		assertTrue(configs.isDisabled("Removed"));
		assertFalse(configs.isDisabled("Test"));
		assertFalse(configs.isDisabled("General"));
	}

	@Test(expected = ConfigurationException.class)
	public void testMandatory() throws Exception {
		new TestMandatory(configs);
	}

}
