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

/**
 * Implemented according to:
 * 
 * <pre>
 * Kasai, Toru, et al.
 * "Linear-time longest-common-prefix computation in suffix arrays and its applications."
 * Combinatorial Pattern Matching. Springer Berlin Heidelberg, 2001.
 * </pre>
 */
public class LongestCommonPrefix
{
	public static int[] compute(int[] seq, int n, int[] suffixArray)
	{
		int[] rank = new int[n];
		int[] height = new int[n];
		for (int i = 0; i < n; ++i)
			rank[suffixArray[i]] = i;
		
		int h = 0;
		for (int i = 0; i < n; ++i)
		{
			if (rank[i] > 0)
			{
				int j = suffixArray[rank[i] - 1];
				while (seq[i + h] == seq[j + h])
					h = h + 1;
				
				height[rank[i]] = h;
				if (h > 0)
					h = h - 1;
			}
		}
		
		return height;
	}
	
	public static int[] compute(
			int[] seq,
			int n,
			int valueDomainStart,
			int[] suffixArray)
	{
		int[] rank = new int[n];
		int[] height = new int[n];
		for (int i = 0; i < n; ++i)
			rank[suffixArray[i]] = i;
		
		int h = 0;
		for (int i = 0; i < n; ++i)
		{
			if (rank[i] > 0)
			{
				int j = suffixArray[rank[i] - 1];
				while ((seq[i + h] == seq[j + h]) && (seq[i + h] >= valueDomainStart))
					h = h + 1;
				
				height[rank[i]] = h;
				if (h > 0)
					h = h - 1;
			}
		}
		
		return height;
	}
}
