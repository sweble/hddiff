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

public class SimpleSubstringJudge
		implements
			SubstringJudgeInterface<String>
{
	private final int minStringLength;
	
	public SimpleSubstringJudge(int minStringLength)
	{
		this.minStringLength = minStringLength;
	}
	
	@Override
	public boolean isValid(String str, int start, int len)
	{
		return (len >= getMinLength());
	}
	
	@Override
	public int getMinLength()
	{
		return minStringLength;
	}
}
