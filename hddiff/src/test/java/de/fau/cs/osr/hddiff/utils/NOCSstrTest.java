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

import java.util.List;

import org.junit.Test;

import de.fau.cs.osr.hddiff.utils.NOCSstr.CommonSubstring;

public class NOCSstrTest
{
	private final SubstringJudgeInterface<String> subStringJudge =
			new SubstringJudgeInterface<String>()
			{
				@Override
				public boolean isValid(String seq, int start, int len)
				{
					return true;
				}
				
				@Override
				public int getMinLength()
				{
					return 6;
				}
			};
	
	// =========================================================================
	
	@Test
	public void testName() throws Exception
	{
		String str1 = "RambazambaabcHello Worlddef";
		String str2 = "defHello WorldghiblaRambazamba";
		List<CommonSubstring> result = nocss(str1, str2);
		
		assertEquals(2, result.size());
		assertTrue(contains(str1, result, "Rambazamba"));
		assertTrue(contains(str1, result, "Hello World"));
	}
	
	// =========================================================================
	
	private boolean contains(
			String str1,
			List<CommonSubstring> result,
			String needle)
	{
		for (CommonSubstring cs : result)
			if (str1.substring(cs.start1, cs.start1 + cs.len).equals(needle))
				return true;
		return false;
	}
	
	private List<CommonSubstring> nocss(String str1, String str2)
	{
		List<CommonSubstring> result = NOCSstr.compute(str1, str2, NOCSstr.MARKER_SEQ_D_MIN, subStringJudge, null);
		return result;
	}
}
