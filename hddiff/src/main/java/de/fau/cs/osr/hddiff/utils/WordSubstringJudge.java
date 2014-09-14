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

public class WordSubstringJudge
		implements
			SubstringJudgeInterface<String>
{
	private final int minWordCount;
	
	private final int minLength;
	
	// =========================================================================
	
	public WordSubstringJudge(int minStrLength, int minWordCount)
	{
		if ((minWordCount < 2) || (minStrLength < 3))
			throw new IllegalArgumentException("Illegal constraints!");
		
		this.minWordCount = minWordCount;
		this.minLength = minStrLength;
	}
	
	// =========================================================================
	
	@Override
	public boolean isValid(String seq, int start, int len)
	{
		if (len <= getMinLength())
			return false;
		
		int words = 0;
		boolean wasSep = isSep(seq.charAt(start));
		for (int i = start + 1; i < start + len; ++i)
		{
			boolean isSep = isSep(seq.charAt(i));
			if (!wasSep && isSep)
			{
				++words;
				if (words >= minWordCount)
					return true;
			}
			wasSep = isSep;
		}
		if (!wasSep)
			// We're still processing the last word -> count end of string as separator.
			++words;
		return (words >= minWordCount);
	}
	
	@Override
	public int getMinLength()
	{
		return minLength;
	}
	
	// =========================================================================
	
	private boolean isSep(char ch)
	{
		return Character.isWhitespace(ch) || (ch == '-') || (ch == '_') || (ch == '.');
	}
}
