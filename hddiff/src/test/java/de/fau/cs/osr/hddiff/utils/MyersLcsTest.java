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
package de.fau.cs.osr.hddiff.utils;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

public class MyersLcsTest
{
	private LcsMyers<Character> lcs =
			new LcsMyers<>(new ElementComparatorInterface<Character>()
			{
				@Override
				public boolean equals(Character a, Character b)
				{
					return a == b;
				}
			});
	
	// =========================================================================
	
	@Test
	public void testOneEmptySeq() throws Exception
	{
		assertEquals(6, lcs.lcs(
				toCharArray(""),
				toCharArray("CBABAC"),
				Integer.MAX_VALUE));
		assertTrue(lcs.getLcs().isEmpty());
		
		assertEquals(7, lcs.lcs(
				toCharArray("CBABAC"),
				toCharArray("1"),
				Integer.MAX_VALUE));
		assertTrue(lcs.getLcs().isEmpty());
	}
	
	@Test
	public void testSeqEqual() throws Exception
	{
		assertEquals(0, lcs.lcs(
				toCharArray("ABC"),
				toCharArray("ABC"),
				Integer.MAX_VALUE));
		assertEquals(toCharArray("AABBCC"), lcs.getLcs());
	}
	
	@Test
	public void testTrimmedEqualNoLcs() throws Exception
	{
		assertEquals(4, lcs.lcs(
				toCharArray("abcxxxdef"),
				toCharArray("abcydef"),
				Integer.MAX_VALUE));
		assertEquals(toCharArray("aabbccddeeff"), lcs.getLcs());
		
		assertEquals(3, lcs.lcs(
				toCharArray("abcxxxdef"),
				toCharArray("abcdef"),
				Integer.MAX_VALUE));
		assertEquals(toCharArray("aabbccddeeff"), lcs.getLcs());
		
		assertEquals(3, lcs.lcs(
				toCharArray("abcdef"),
				toCharArray("abcyyydef"),
				Integer.MAX_VALUE));
		assertEquals(toCharArray("aabbccddeeff"), lcs.getLcs());
	}
	
	@Test
	public void testName() throws Exception
	{
		assertEquals(5, lcs.lcs(
				toCharArray("ABCABBA"),
				toCharArray("CBABAC"),
				Integer.MAX_VALUE));
		assertEquals(toCharArray("CCAABBAA"), lcs.getLcs());
	}
	
	@Test
	public void testName2() throws Exception
	{
		assertEquals(5, lcs.lcs(
				toCharArray("AABCABBA"),
				toCharArray("ACBABAC"),
				Integer.MAX_VALUE));
		assertEquals(toCharArray("AACCAABBAA"), lcs.getLcs());
	}
	
	// =========================================================================
	
	private ArrayList<Character> toCharArray(String string)
	{
		ArrayList<Character> a = new ArrayList<>(string.length());
		for (char ch : string.toCharArray())
			a.add(ch);
		return a;
	}
}
