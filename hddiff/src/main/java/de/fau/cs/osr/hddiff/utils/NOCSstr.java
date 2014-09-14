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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.fau.cs.osr.hddiff.utils.ReportItem.Timer;

/**
 * Greedy longest non-overlapping common sub-strings
 */
public class NOCSstr
{
	private static final int MARKER_SEQ_END = 0;
	
	private static final int MARKER_SEQ_SEP = 1;
	
	/** Array/String value domain (min incl.) */
	public static final int MARKER_SEQ_D_MIN = 2;
	
	/** Array/String value domain (max incl.) */
	public static final int MARKER_SEQ_D_MAX = Integer.MAX_VALUE;
	
	// =========================================================================
	
	/**
	 * The strings must not contain the characters \u0000 or \u0001
	 */
	public static List<CommonSubstring> compute(
			String str1,
			String str2,
			int valueDomainStart,
			SubstringJudgeInterface<String> subStringJudge,
			ReportItem ri)
	{
		Timer t0 = null;
		if (ri != null)
			t0 = ri.startTimer("00.03.01a) str1 $1 str2 $0");
		
		int n1 = str1.length();
		int n2 = str2.length();
		int minLen = subStringJudge.getMinLength();
		if ((n1 < minLen) || (n2 < minLen))
			return Collections.emptyList();
		
		Map<Character, Integer> m = new HashMap<>(1024);
		
		// ---- Stitch input together
		
		int k = valueDomainStart;
		int[] input = new int[n1 + 1 + n2 + 1 + 3];
		for (int i = 0; i < n1; ++i)
		{
			char ch = str1.charAt(i);
			if (ch >= valueDomainStart)
			{
				Integer code = m.get(ch);
				if (code == null)
				{
					code = k++;
					m.put(ch, code);
				}
				input[i] = code;
			}
			else
			{
				input[i] = ch;
			}
		}
		
		input[n1] = MARKER_SEQ_SEP;
		
		int s2 = n1 + 1;
		for (int i = 0; i < n2; ++i)
		{
			char ch = str2.charAt(i);
			if (ch >= valueDomainStart)
			{
				Integer code = m.get(ch);
				if (code == null)
				{
					code = k++;
					m.put(ch, code);
				}
				input[s2 + i] = code;
			}
			else
			{
				input[s2 + i] = ch;
			}
		}
		int n12 = s2 + n2;
		
		input[n12] = MARKER_SEQ_END;
		++n12;
		
		// ----
		
		if (t0 != null)
			t0.stop();
		
		// ---- Do actual work
		
		LinkedList<CommonSubstring> result = compute(input, str1, n1, n2, k, minLen, valueDomainStart, subStringJudge, ri);
		
		/*
		System.out.println(StringEscapeUtils.escapeJava(str1));
		System.out.println(StringEscapeUtils.escapeJava(str2));
		System.out.println();
		
		for (CommonSubstring cs : result)
		{
			System.out.println(cs);
			System.out.println(StringEscapeUtils.escapeJava(str1.substring(cs.start1, cs.start1 + cs.len)));
			System.out.println();
		}
		*/
		
		return result;
	}
	
	/**
	 * The strings must not contain the value 0 or 1
	 */
	public static List<CommonSubstring> compute(
			ArrayList<Integer> seq1,
			ArrayList<Integer> seq2,
			int valueDomainStart,
			SubstringJudgeInterface<ArrayList<Integer>> subStringJudge,
			ReportItem ri)
	{
		Timer t0 = null;
		if (ri != null)
			t0 = ri.startTimer("00.03.02a) str1 $1 str2 $0");
		
		int n1 = seq1.size();
		int n2 = seq2.size();
		int minLen = subStringJudge.getMinLength();
		if ((n1 < minLen) || (n2 < minLen))
			return Collections.emptyList();
		
		// ---- Stitch input together
		
		int k = 0;
		int[] input = new int[n1 + 1 + n2 + 1 + 3];
		for (int i = 0; i < n1; ++i)
		{
			int x = input[i] = seq1.get(i);
			if (x > k)
				k = x;
		}
		input[n1] = MARKER_SEQ_SEP;
		int s2 = n1 + 1;
		for (int i = 0; i < n2; ++i)
		{
			int x = input[s2 + i] = seq2.get(i);
			if (x > k)
				k = x;
		}
		int n12 = s2 + n2;
		input[n12] = MARKER_SEQ_END;
		++n12;
		++k;
		
		if (t0 != null)
			t0.stop();
		
		// ---- Do actual work
		
		return compute(input, seq1, n1, n2, k, minLen, valueDomainStart, subStringJudge, ri);
	}
	
	// =========================================================================
	
	private static <T> LinkedList<CommonSubstring> compute(
			int[] input,
			T seq1,
			int n1,
			int n2,
			int k,
			int minLen,
			int valueDomainStart,
			SubstringJudgeInterface<T> subStringJudge,
			ReportItem ri)
	{
		int n12 = n1 + 1 + n2 + 1;
		
		Timer t0 = null;
		if (ri != null)
			t0 = ri.startTimer("00.03.01b) SA");
		
		int[] sa = SuffixArray.compute(input, n12, k);
		
		if (t0 != null)
			t0.stop();
		
		Timer t1 = null;
		if (ri != null)
			t1 = ri.startTimer("00.03.01c) LCP");
		
		int[] lcp = LongestCommonPrefix.compute(input, n12, valueDomainStart, sa);
		
		if (t1 != null)
			t1.stop();
		
		Timer t2 = null;
		if (ri != null)
			t2 = ri.startTimer("00.03.01d) bucketSort");
		
		LinkedList<CommonSubstring>[] buckets =
				bucketSort(n1, n2, sa, lcp, minLen);
		
		if (t2 != null)
			t2.stop();
		
		Timer t3 = null;
		if (ri != null)
			t3 = ri.startTimer("00.03.01e) greedyCover");
		
		LinkedList<CommonSubstring> greedyCover = greedyCover(n1, n2, buckets, minLen, seq1, subStringJudge);
		
		if (t3 != null)
			t3.stop();
		
		return greedyCover;
	}
	
	private static <T> LinkedList<CommonSubstring>[] bucketSort(
			int n1,
			int n2,
			int[] sa,
			int[] lcp,
			int minLen)
	{
		@SuppressWarnings("unchecked")
		LinkedList<CommonSubstring>[] buckets = new LinkedList[Math.min(n1, n2) + 1];
		
		for (int i = 0; i < lcp.length; ++i)
		{
			int len = lcp[i];
			if (len < minLen)
				continue;
			
			int start1 = sa[i - 1];
			int start2 = sa[i];
			
			// Skip duplicates
			int j = i + 1;
			while ((j < lcp.length) && (lcp[j] == len))
				++j;
			if (j > i + 1)
			{
				i = j - 1;
				continue;
			}
			
			// Strings must be from both halves
			boolean both = (start1 < n1) ^ (start2 < n1);
			if (!both)
				continue;
			
			if (start2 < start1)
			{
				int tmp = start2;
				start2 = start1;
				start1 = tmp;
			}
			
			// Correct position to be relative to str2
			start2 -= n1 + 1;
			
			getBucketForMod(buckets, len).add(
					new CommonSubstring(start1, start2, len));
		}
		
		return buckets;
	}
	
	private static LinkedList<CommonSubstring> getBucketForMod(
			LinkedList<CommonSubstring>[] buckets, int i)
	{
		LinkedList<CommonSubstring> bucket = buckets[i];
		if (bucket == null)
			bucket = buckets[i] = new LinkedList<>();
		return bucket;
	}
	
	private static <T> LinkedList<CommonSubstring> greedyCover(
			int n1,
			int n2,
			LinkedList<CommonSubstring>[] buckets,
			int minLen,
			T seq1,
			SubstringJudgeInterface<T> ssj)
	{
		boolean covered1[] = new boolean[n1];
		boolean covered2[] = new boolean[n2];
		
		LinkedList<CommonSubstring> result = new LinkedList<>();
		
		for (int i = buckets.length - 1; i >= minLen; --i)
		{
			LinkedList<CommonSubstring> bucket = buckets[i];
			if (bucket == null)
				continue;
			
			for (CommonSubstring cs : bucket)
			{
				if (covered1[cs.start1] || covered2[cs.start2])
					// This one is overlapping with an already accepted substring
					// (and is therefore just a substring of the accepted substring)
					continue;
				
				if (!ssj.isValid(seq1, cs.start1, cs.len))
					continue;
				
				for (int j = 0; j < cs.len; ++j)
				{
					int k1 = cs.start1 + j;
					int k2 = cs.start2 + j;
					if (covered1[k1] || covered2[k2])
					{
						// This one runs into another accepted substring
						// It cannot be a longer prefix to that substring because we sorted by size and
						// are already done with the longer substring. Therefore this substring is only 
						// a prefix in one of the strings but not in the other.
						cs.len = j;
						break;
					}
					covered1[k1] = covered2[k2] = true;
				}
				
				result.add(cs);
			}
		}
		
		return result;
	}
	
	// =========================================================================
	
	public static final class CommonSubstring
			implements
				Comparable<CommonSubstring>
	{
		public int start1;
		
		public int start2;
		
		public int len;
		
		public CommonSubstring(int start1, int start2, int len)
		{
			this.start1 = start1;
			this.start2 = start2;
			this.len = len;
		}
		
		@Override
		public int compareTo(CommonSubstring o)
		{
			return Integer.compare(start1, o.start1);
		}
		
		@Override
		public String toString()
		{
			return "CommonSubstring [start1=" + start1 + ", start2=" + start2 + ", len=" + len + "]";
		}
	}
}
