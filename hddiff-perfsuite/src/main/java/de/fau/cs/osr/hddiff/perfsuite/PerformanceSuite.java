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
package de.fau.cs.osr.hddiff.perfsuite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

import de.fau.cs.osr.hddiff.perfsuite.util.PageRevText;
import de.fau.cs.osr.hddiff.utils.Report;
import de.fau.cs.osr.hddiff.utils.ReportItem;

public class PerformanceSuite
{
	/** Do some warmup comparisons first before taking measurements. */
	private static final boolean WARM_UP = true;
	
	/** Process whole input an take measurements. */
	private static final boolean SERIOUS = true;
	
	/** Also compare HdDiff with FcDiff and XyDiff. */
	private static final boolean COMPARE = true;
	
	/** DEBUG: Produce a pretty printed WOM version during diff generation. */
	private static final boolean PRETTY_PRINT_WOM = false;
	
	/** DEBUG: Report additional events. */
	private static final int LOG_LEVEL_INFO = ReportItem.LOG_LEVEL_INFO;
	
	/** Number of warmup iterations when WARM_UP is set to true. */
	private static final int WARMUP_ITERATIONS = 1000;
	
	/** How often should the report get saved? */
	private static final int SAVE_AND_TELL_EVERY = 10000;
	
	/** How many progress dots per line? */
	private static final int DOTS_PER_LINE = 50;
	
	// =========================================================================
	
	private final Gson gson = new Gson();
	
	private final Random rnd = new Random();
	
	private Report report;
	
	private int itemIndex;
	
	private int completed;
	
	private AtomicInteger errors;
	
	private String currentSet;
	
	// =========================================================================
	
	private String dirBase;
	
	private List<String> parsedSets;
	
	// =========================================================================
	
	public static void main(String[] args) throws Exception
	{
		new PerformanceSuite().run(args);
	}
	
	// =========================================================================
	
	private void run(String[] args) throws Exception
	{
		if (args.length < 2)
		{
			System.err.println("Usage: ... DIR_BASE SET [SET..]");
			System.exit(1);
		}
		else
		{
			dirBase = args[0];
			parsedSets = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
			
			if (WARM_UP)
			{
				System.out.println("Warming up...");
				processSets(WARMUP_ITERATIONS, 0, true, false);
			}
			
			if (SERIOUS)
			{
				System.out.println("Go!");
				processSets(Integer.MAX_VALUE, 0, false, true);
			}
		}
	}
	
	private void processSets(
			int limit,
			int skip,
			boolean warmUp,
			boolean writeReport) throws Exception
	{
		for (String currentSet : parsedSets)
		{
			this.errors = new AtomicInteger();
			this.report = new Report();
			this.currentSet = currentSet;
			this.itemIndex = 0;
			this.completed = 0;
			
			processSet(limit, skip, warmUp, writeReport);
		}
	}
	
	private void processSet(
			int limit,
			int skip,
			boolean warmUp,
			boolean writeReport) throws Exception
	{
		String fnameBase = dirBase + currentSet;
		File revFileParsed = new File(fnameBase + "-parsed.json");
		try (BufferedReader brParsed = new BufferedReader(new FileReader(revFileParsed)))
		{
			File revFileRaw = new File(fnameBase + ".json");
			try (BufferedReader brRaw = new BufferedReader(new FileReader(revFileRaw)))
			{
				ArrayList<Job> jobs = new ArrayList<>();
				
				String lineParsed;
				while ((lineParsed = brParsed.readLine()) != null)
				{
					if (skip > 0)
					{
						--skip;
						continue;
					}
					
					--limit;
					if (limit < 0)
						break;
					
					PageRevText prtParsed = retrieveText(lineParsed);
					PageRevText prtRaw = retrieveRaw(brRaw, prtParsed);
					jobs.add(new Job(prtParsed, prtRaw));
					
					if (jobs.size() > 20)
						processJobs(jobs, warmUp, writeReport);
				}
				
				processJobs(jobs, warmUp, writeReport);
			}
		}
		
		writeReport(writeReport);
	}
	
	// =========================================================================
	
	/**
	 * Slightly randomize order in which jobs are processed.
	 */
	private void processJobs(
			ArrayList<Job> jobs,
			boolean warmUp,
			boolean writeReport) throws Exception
	{
		while (!jobs.isEmpty())
		{
			int jobId = rnd.nextInt(jobs.size());
			Job job = jobs.remove(jobId);
			processOne(itemIndex, job.prtRaw, job.prtParsed, warmUp, writeReport);
			++itemIndex;
		}
	}
	
	private void processOne(
			int index,
			final PageRevText prtRaw,
			final PageRevText prtParsed,
			boolean warmUp,
			boolean writeReport) throws Exception
	{
		TaskProcessor p = new TaskProcessor(
				errors,
				warmUp,
				COMPARE,
				PRETTY_PRINT_WOM,
				LOG_LEVEL_INFO);
		
		ReportItem reportItem = p.processPair(prtRaw, prtParsed);
		
		finish(reportItem, writeReport);
	}
	
	private void finish(ReportItem reportItem, boolean writeReport) throws IOException
	{
		report.add(reportItem);
		
		++completed;
		
		System.out.print('.');
		System.out.flush();
		
		if (completed % DOTS_PER_LINE == 0)
			System.out.println(completed);
		
		if (completed % SAVE_AND_TELL_EVERY == 0)
		{
			System.out.println(completed);
			writeReport(writeReport);
		}
	}
	
	private void writeReport(boolean write) throws IOException
	{
		if (write)
		{
			File reportFile = new File(dirBase, currentSet + "-report.csv");
			report.writeCsv(reportFile, Locale.US, "UTF8");
		}
	}
	
	// =========================================================================
	
	private PageRevText retrieveText(String lineParsed)
	{
		return gson.fromJson(lineParsed, PageRevText.class);
	}
	
	private PageRevText retrieveRaw(BufferedReader brRaw, PageRevText prtParsed) throws IOException
	{
		PageRevText prtRaw = null;
		
		/**
		 * The conversion of some revisions from markup into WOM failed.
		 * Therefore, there might be more raw text revisions stored than there
		 * are parsed revisions. Since the order of the parsed revisions is the
		 * same as for the raw revisions we can simple skip raw revisions for
		 * which no parsed revisions could be generated.
		 */
		String lineRaw;
		while ((lineRaw = brRaw.readLine()) != null)
		{
			prtRaw = retrieveText(lineRaw);
			if (prtRaw.revA.getId().equals(prtParsed.revA.getId()) &&
					prtRaw.revB.getId().equals(prtParsed.revB.getId()))
				break;
			
			report("Jumped raw revision");
		}
		
		if (prtRaw == null)
			throw new AssertionError("No matching raw revision found!");
		
		return prtRaw;
	}
	
	private void report(String msg)
	{
		System.out.println();
		System.out.println(msg);
	}
	
	// =========================================================================
	
	private static final class Job
	{
		PageRevText prtParsed;
		
		PageRevText prtRaw;
		
		// ----------------
		
		public Job(PageRevText prtParsed, PageRevText prtRaw)
		{
			this.prtParsed = prtParsed;
			this.prtRaw = prtRaw;
		}
	}
}
