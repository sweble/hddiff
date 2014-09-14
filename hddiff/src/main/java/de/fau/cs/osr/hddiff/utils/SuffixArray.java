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
 * Find the suffix array SA of s[0..n-1] in {1..K}^n.
 * 
 * <p>
 * Requires s[n] = s[n+1] = s[n+2] = 0, n >= 2.
 * 
 * <p>
 * Implemented according to:
 * 
 * <pre>
 * Kärkkäinen, Juha, and Peter Sanders.
 * "Simple linear work suffix array construction."
 * Automata, Languages and Programming. Springer Berlin Heidelberg, 2003. 943-955.
 * </pre>
 * 
 * Source code translated from C to Java by Hannes Dohrn. Original C sources
 * were found at <a
 * href="http://people.mpi-inf.mpg.de/~sanders/programs/suffix/drittel.C"
 * >http://people.mpi-inf.mpg.de/~sanders/programs/suffix/drittel.C</a>
 */
public class SuffixArray
{
	/**
	 * @param seq
	 *            Needs a padding of three elements at the end!
	 * @param len
	 *            Length of seq without padding elements.
	 * @param k
	 *            Size of the alphabet.
	 * @return The suffix array plus three padding elements at the end that can
	 *         be ignored.
	 */
	public static int[] compute(int[] seq, int len, int k)
	{
		int n = seq.length - 3;
		if (len != n)
			throw new IllegalArgumentException();
		
		int[] suffixArray = new int[n + 3];
		
		suffixArrayInternal(seq, suffixArray, n, k);
		
		return suffixArray;
	}
	
	static void suffixArrayInternal(int[] s, int[] SA, int n, int K)
	{
		int n0 = (n + 2) / 3;
		int n1 = (n + 1) / 3;
		int n2 = n / 3;
		int n02 = n0 + n2;
		
		int[] s12 = new int[n02 + 3];
		s12[n02] = s12[n02 + 1] = s12[n02 + 2] = 0;
		
		int[] SA12 = new int[n02 + 3];
		SA12[n02] = SA12[n02 + 1] = SA12[n02 + 2] = 0;
		
		int[] s0 = new int[n0];
		
		int[] SA0 = new int[n0];
		
		// generate positions of mod 1 and mod  2 suffixes
		// the "+(n0-n1)" adds a dummy mod 1 suffix if n%3 == 1
		for (int i = 0, j = 0; i < n + (n0 - n1); i++)
			if (i % 3 != 0)
				s12[j++] = i;
		
		// lsb radix sort the mod 1 and mod 2 triples
		radixPass(s12, SA12, s, 2, n02, K);
		radixPass(SA12, s12, s, 1, n02, K);
		radixPass(s12, SA12, s, 0, n02, K);
		
		// find lexicographic names of triples
		int name = 0, c0 = -1, c1 = -1, c2 = -1;
		for (int i = 0; i < n02; i++)
		{
			if (s[SA12[i]] != c0 || s[SA12[i] + 1] != c1 || s[SA12[i] + 2] != c2)
			{
				name++;
				c0 = s[SA12[i]];
				c1 = s[SA12[i] + 1];
				c2 = s[SA12[i] + 2];
			}
			if (SA12[i] % 3 == 1)
			{
				s12[SA12[i] / 3] = name;
			} // left half
			else
			{
				s12[SA12[i] / 3 + n0] = name;
			} // right half
		}
		
		// recurse if names are not yet unique
		if (name < n02)
		{
			suffixArrayInternal(s12, SA12, n02, name);
			// store unique names in s12 using the suffix array 
			for (int i = 0; i < n02; i++)
				s12[SA12[i]] = i + 1;
		}
		else
			// generate the suffix array of s12 directly
			for (int i = 0; i < n02; i++)
				SA12[s12[i] - 1] = i;
		
		// stably sort the mod 0 suffixes from SA12 by their first character
		for (int i = 0, j = 0; i < n02; i++)
			if (SA12[i] < n0)
				s0[j++] = 3 * SA12[i];
		radixPass(s0, SA0, s, 0, n0, K);
		
		// merge sorted SA0 suffixes and sorted SA12 suffixes
		for (int p = 0, t = n0 - n1, k = 0; k < n; k++)
		{
			int i = getI(SA12, t, n0); // pos of current offset 12 suffix
			int j = SA0[p]; // pos of current offset 0  suffix
			if (SA12[t] < n0 ?
					leq(s[i], s12[SA12[t] + n0], s[j], s12[j / 3]) :
					leq(s[i], s[i + 1], s12[SA12[t] - n0 + 1], s[j], s[j + 1], s12[j / 3 + n0]))
			{
				// suffix from SA12 is smaller
				SA[k] = i;
				t++;
				if (t == n02)
				{
					// done --- only SA0 suffixes left
					for (k++; p < n0; p++, k++)
						SA[k] = SA0[p];
				}
			}
			else
			{
				SA[k] = j;
				p++;
				if (p == n0)
				{
					// done --- only SA12 suffixes left
					for (k++; t < n02; t++, k++)
						SA[k] = getI(SA12, t, n0);
				}
			}
		}
	}
	
	/**
	 * Stably sort a[0..n-1] to b[0..n-1] with keys in 0..K from r[x + rOfs].
	 */
	private static void radixPass(
			int[] a,
			int[] b,
			int[] r,
			int rOfs,
			int n,
			int K)
	{
		int[] c = new int[K + 1];
		
		for (int i = 0; i < n; i++)
			c[r[a[i] + rOfs]]++;
		
		for (int i = 0, sum = 0; i <= K; i++)
		{
			int t = c[i];
			c[i] = sum;
			sum += t;
		}
		
		for (int i = 0; i < n; i++)
			b[c[r[a[i] + rOfs]]++] = a[i];
	}
	
	private static int getI(int[] SA12, int t, int n0)
	{
		return (SA12[t] < n0 ? SA12[t] * 3 + 1 : (SA12[t] - n0) * 3 + 2);
	}
	
	private static boolean leq(int a1, int a2, int b1, int b2)
	{
		return (a1 < b1 || a1 == b1 && a2 <= b2);
	}
	
	private static boolean leq(int a1, int a2, int a3, int b1, int b2, int b3)
	{
		return (a1 < b1 || a1 == b1 && leq(a2, a3, b2, b3));
	}
}
