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

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Implemented according to
 * 
 * <pre>
 * Myers, Eugene W.
 * "An O(ND) difference algorithm and its variations."
 * Algorithmica 1.1-4 (1986): 251-266.
 * </pre>
 * 
 * Other great learning resources:
 * <ul>
 * <li><a href="http://www.codeproject.com/Articles/42279/Investigating-Myers-diff-algorithm-Part-of">Investigating Myers' Diff Algorithm: Part 1 of 2</a></li>
 * <li><a href="http://www.codeproject.com/Articles/42280/Investigating-Myers-Diff-Algorithm-Part-of">Investigating Myers' Diff Algorithm: Part 2 of 2</a></li>
 * </ul>
 */
public class LcsMyers<T>
{
	private final ElementComparatorInterface<T> cmp;
	
	private ArrayList<int[]> vs;
	
	private ArrayList<T> lcs;
	
	// =========================================================================
	
	public LcsMyers(ElementComparatorInterface<T> cmp)
	{
		this.cmp = cmp;
	}
	
	// =========================================================================
	
	public final int lcs(ArrayList<T> a, ArrayList<T> b)
	{
		return lcs(a, b, a.size() + b.size());
	}
	
	public final int lcs(ArrayList<T> a, ArrayList<T> b, int max)
	{
		int n = a.size();
		int m = b.size();
		if (max < 0)
			throw new IllegalArgumentException();
		
		int min = Math.min(n, m);
		if (lcs == null)
			lcs = new ArrayList<T>(2 * min);
		else
			lcs.clear();
		
		if (min == 0)
			return n + m;
		
		int trimFront = 0;
		for (; trimFront < min; ++trimFront)
		{
			T aItem = a.get(trimFront);
			T bItem = b.get(trimFront);
			if (!cmp.equals(aItem, bItem))
				break;
			lcs.add(aItem);
			lcs.add(bItem);
		}
		
		int trimEndMax = min - trimFront;
		int trimEnd = 0;
		for (; trimEnd < trimEndMax; ++trimEnd)
		{
			T aItem = a.get(n - trimEnd - 1);
			T bItem = b.get(m - trimEnd - 1);
			if (!cmp.equals(aItem, bItem))
				break;
		}
		
		int d;
		int trim = trimFront + trimEnd;
		if (trim < min)
		{
			int n2 = n - trim;
			int m2 = m - trim;
			max = Math.min(max, n2 + m2);
			
			if (vs == null)
				vs = new ArrayList<>(max + 1);
			else
				vs.clear();
			
			d = lcs(a, b, trimFront, n2, m2, max);
			
			backtrack(a, b, trimFront, n2, m2, max);
		}
		else
		{
			d = n + m - min * 2;
		}
		
		for (int i = trimEnd; i > 0; --i)
		{
			lcs.add(a.get(n - i));
			lcs.add(b.get(m - i));
		}
		
		return d;
	}
	
	private int lcs(
			ArrayList<T> a,
			ArrayList<T> b,
			int from,
			int n2,
			int m2,
			int max)
	{
		// [-MAX ... +MAX]
		int[] v = new int[2 * max + 1];
		
		// That's done by the JVM anyway...
		//v[1 + MAX] = 0;
		
		int d;
		forD: for (d = 0; d <= max; ++d)
		{
			vs.add(Arrays.copyOf(v, v.length));
			
			for (int k = -d; k <= d; k += 2)
			{
				int kIdx = k + max;
				int x = ((k == -d) || ((k != +d) && v[kIdx - 1] < v[kIdx + 1])) ?
						// go down from k+1, x doesn't change
						(v[kIdx + 1]) :
						// go right form k-1, x += 1
						(v[kIdx - 1] + 1);
				
				int y = x - k;
				
				while ((x < n2) && (y < m2) && (cmp.equals(a.get(from + x), b.get(from + y))))
				{
					++x;
					++y;
				}
				
				if ((x >= n2) && (y >= m2))
					break forD;
				
				v[kIdx] = x;
			}
		}
		
		return d;
	}
	
	private void backtrack(
			ArrayList<T> a,
			ArrayList<T> b,
			int from,
			int n,
			int m,
			int max)
	{
		int x = n;
		int y = m;
		int d = vs.size() - 1;
		
		int j = 0;
		Object[] revResult = new Object[Math.min(n, m) * 2];
		
		for (; (x > 0) || (y > 0); --d)
		{
			int[] v = vs.get(d);
			
			int k = x - y;
			
			int prevK;
			int snakeX;
			if ((k == -d) || ((k != +d) && v[k - 1 + max] < v[k + 1 + max]))
			{
				prevK = k + 1;
				snakeX = 0;
			}
			else
			{
				prevK = k - 1;
				snakeX = 1;
			}
			
			int prevX = v[prevK + max];
			int prevY = prevX - prevK;
			snakeX += prevX;
			
			while (x > snakeX)
			{
				revResult[j++] = b.get(--y + from);
				revResult[j++] = a.get(--x + from);
			}
			
			x = prevX;
			y = prevY;
		}
		
		for (--j; j >= 0; --j)
		{
			@SuppressWarnings("unchecked")
			T item = (T) revResult[j];
			lcs.add(item);
		}
	}
	
	public ArrayList<T> getLcs()
	{
		return lcs;
	}
}
