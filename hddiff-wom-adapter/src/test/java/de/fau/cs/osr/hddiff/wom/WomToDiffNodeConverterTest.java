/**
 * Copyright 2011 The Open Source Research Group,
 *                University of Erlangen-Nürnberg
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package de.fau.cs.osr.hddiff.wom;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import de.fau.cs.osr.hddiff.wom.WomToDiffNodeConverter;

public class WomToDiffNodeConverterTest
{
	@Test
	public void testName() throws Exception
	{
		Pattern rx = Pattern.compile("[^\\s\\[]");
		assertTrue(rx.matcher("x").matches());
		assertFalse(rx.matcher(" ").matches());
		assertFalse(rx.matcher(" [").matches());
	}
	
	@Test
	public void testSimpleLink() throws Exception
	{
		String wt = "[[target]]";
		Matcher m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
		
		wt = " [[target]]";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
		
		wt = "[[ target]]";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
		
		wt = " [[ target]]";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
		
		wt = " [[ target ]]";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
		
		wt = " [[ target|...";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
		
		wt = " [[ target";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
		
		wt = " [[ target |";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertNull(m.group(1));
		assertEquals("target", m.group(2));
	}
	
	@Test
	public void testPrefixedLink()
	{
		String wt = "a[[target]]";
		Matcher m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertEquals("a", m.group(1));
		assertEquals("target", m.group(2));
		
		wt = " some[[ target ]]s";
		m = WomToDiffNodeConverter.LINK_OPEN_RX.matcher(wt);
		assertTrue(m.find());
		assertEquals("some", m.group(1));
		assertEquals("target", m.group(2));
	}
	
	@Test
	public void testPostfixedLink()
	{
		String wt = " some[[ target ]]s";
		Matcher m = WomToDiffNodeConverter.LINK_CLOSE_RX.matcher(wt);
		assertTrue(m.find());
		assertEquals("s", m.group(1));
		
		wt = " some[[ target ]]s ";
		m = WomToDiffNodeConverter.LINK_CLOSE_RX.matcher(wt);
		assertTrue(m.find());
		assertEquals("s", m.group(1));
		
		wt = " some[[ target ]] s";
		m = WomToDiffNodeConverter.LINK_CLOSE_RX.matcher(wt);
		assertFalse(m.find());
	}
}
