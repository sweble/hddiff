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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;

import com.sksamuel.diffpatch.DiffMatchPatch;
import com.sksamuel.diffpatch.DiffMatchPatch.Diff;

import de.fau.cs.osr.hddiff.HDDiff;
import de.fau.cs.osr.hddiff.HDDiffOptions;
import de.fau.cs.osr.hddiff.editscript.EditOp;
import de.fau.cs.osr.hddiff.editscript.EditScriptManager;
import de.fau.cs.osr.hddiff.perfsuite.RunFcDiff.FcDiffException;
import de.fau.cs.osr.hddiff.perfsuite.RunXyDiff.XyDiffException;
import de.fau.cs.osr.hddiff.perfsuite.util.PageRevDiffNode;
import de.fau.cs.osr.hddiff.perfsuite.util.PageRevText;
import de.fau.cs.osr.hddiff.perfsuite.util.PageRevWom;
import de.fau.cs.osr.hddiff.perfsuite.util.SerializationUtils;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.utils.ReportItem;
import de.fau.cs.osr.hddiff.utils.ReportItem.Timer;
import de.fau.cs.osr.hddiff.utils.WordSubstringJudge;
import de.fau.cs.osr.hddiff.wom.WomNodeEligibilityTester;
import de.fau.cs.osr.hddiff.wom.WomNodeMetrics;
import de.fau.cs.osr.utils.ComparisonException;

public class TaskProcessor
{
	private final AtomicInteger errors;
	
	private final boolean warmUp;
	
	private final boolean compare;
	
	private final int logLevelInfo;
	
	private final boolean prettyPrintWom;
	
	private final SerializationUtils serialization;
	
	// =========================================================================
	
	public TaskProcessor(
			AtomicInteger errors,
			boolean warmUp,
			boolean compare,
			boolean prettyPrintWom,
			int logLevelInfo)
	{
		this.errors = errors;
		this.warmUp = warmUp;
		this.compare = compare;
		this.logLevelInfo = logLevelInfo;
		this.prettyPrintWom = prettyPrintWom;
		this.serialization = new SerializationUtils(TaskProcessor.class.getSimpleName());
	}
	
	// =========================================================================
	
	public ReportItem processPair(PageRevText prtRaw, PageRevText prtParsed) throws Exception
	{
		ReportItem reportItem = new ReportItem(logLevelInfo);
		
		reportItem.recordFigure("01.01.a) page", prtParsed.page.getId(), "ID");
		reportItem.recordFigure("01.01.b) namespace", prtParsed.page.getNamespace(), "ID");
		reportItem.recordText("01.01.c) title", prtParsed.page.getTitle());
		reportItem.recordFigure("01.02.a) rev A", prtParsed.revA.getId(), "ID");
		reportItem.recordFigure("01.02.b) rev B", prtParsed.revB.getId(), "ID");
		reportItem.recordFigure("01.02.c) size A", prtRaw.textA.getBytes("UTF8").length, "bytes");
		reportItem.recordFigure("01.02.d) size B", prtRaw.textB.getBytes("UTF8").length, "bytes");
		
		boolean success;
		boolean allSuccess = true;
		
		File fa = null;
		File fb = null;
		if (prettyPrintWom)
		{
			PageRevWom prw = serialization.xmlToWom(prtParsed);
			fa = serialization.storeWomNiceTemp(prw, prw.revA, prw.womA);
			fb = serialization.storeWomNiceTemp(prw, prw.revB, prw.womB);
		}
		
		try
		{
			if (!warmUp && compare)
			{
				success = myersDiff(prtRaw, reportItem, "10.01.b)", "10.02.");
				reportItem.recordFigure("10.01.a) Myers success", success ? 1 : 0, "bool");
				allSuccess &= success;
				
				File compactWomA = null;
				File compactWomB = null;
				try
				{
					PageRevDiffNode prdn = serialization.xmlToDiffNode(prtParsed);
					compactWomA = serialization.storeWomCompactTemp(prdn, prdn.revA, prdn.nodeA);
					compactWomB = serialization.storeWomCompactTemp(prdn, prdn.revB, prdn.nodeB);
					
					success = xyDiff(prdn, compactWomA, compactWomB, reportItem, "11.01.b)", "11.02.");
					reportItem.recordFigure("11.01.a) XyDiff (unsplit) success", success ? 1 : 0, "bool");
					allSuccess &= success;
					
					prdn = serialization.xmlToDiffNode(prtParsed);
					success = fcDiff(prdn, compactWomA, compactWomB, reportItem, "12.01.b)", "12.02.");
					reportItem.recordFigure("12.01.a) FcDiff (unsplit) success", success ? 1 : 0, "bool");
					allSuccess &= success;
				}
				finally
				{
					FileUtils.deleteQuietly(compactWomA);
					FileUtils.deleteQuietly(compactWomB);
				}
			}
			
			{
				PageRevDiffNode prdn = serialization.xmlToDiffNode(prtParsed);
				List<EditOp> editScript = wikiDiff(prdn, reportItem, "", "20.02.");
				success = (editScript != null);
				reportItem.recordFigure("20.01.a) WikiDiff success", success ? 1 : 0, "bool");
				allSuccess &= success;
			}
			
			if (!warmUp && compare)
			{
				File compactWomA = null;
				File compactWomB = null;
				try
				{
					// Use HDDiff to split text nodes.
					PageRevDiffNode prdn = serialization.xmlToDiffNode(prtParsed);
					wikiDiffSplit(prdn);
					compactWomA = serialization.storeWomCompactTemp(prdn, prdn.revA, prdn.nodeA);
					compactWomB = serialization.storeWomCompactTemp(prdn, prdn.revB, prdn.nodeB);
					
					prdn.nodeA.unmapDeep();
					prdn.nodeB.unmapDeep();
					success = xyDiff(prdn, compactWomA, compactWomB, reportItem, "31.01.b)", "31.02.");
					reportItem.recordFigure("31.01.a) XyDiff (split) success", success ? 1 : 0, "bool");
					allSuccess &= success;
					
					prdn.nodeA.unmapDeep();
					prdn.nodeB.unmapDeep();
					success = fcDiff(prdn, compactWomA, compactWomB, reportItem, "32.01.b)", "32.02.");
					reportItem.recordFigure("32.01.a) FcDiff (split) success", success ? 1 : 0, "bool");
					allSuccess &= success;
				}
				finally
				{
					FileUtils.deleteQuietly(compactWomA);
					FileUtils.deleteQuietly(compactWomB);
				}
			}
			
			// ---------------------------------------------------------------------
			
			if (!allSuccess)
				System.out.println(String.format("Errors: %d", errors.incrementAndGet()));
			
			return reportItem;
		}
		catch (Exception e)
		{
			System.out.println("TaskProcessor failed to process a pair: " + e.getMessage());
			System.out.println(String.format("Errors: %d", errors.incrementAndGet()));
			return reportItem;
		}
		finally
		{
			if (prettyPrintWom)
			{
				FileUtils.deleteQuietly(fa);
				FileUtils.deleteQuietly(fb);
			}
		}
	}
	
	// =========================================================================
	
	private List<EditOp> wikiDiff(
			PageRevDiffNode prdn,
			ReportItem reportItem,
			String wikiDiffReportPrefix,
			String editScriptReportId)
	{
		try
		{
			HDDiffOptions options = setupWikiDiff();
			
			DiffNode root1 = prdn.nodeA.getFirstChild();
			DiffNode root2 = prdn.nodeB.getFirstChild();
			
			ReportItem wdReportItem = new ReportItem(wikiDiffReportPrefix, reportItem);
			HDDiff wd = new HDDiff(root1, root2, options, wdReportItem);
			List<EditOp> editScript = wd.editScript();
			
			EditScriptAnalysis esa = new EditScriptAnalysis(editScript);
			
			// MUST BE LAST SINCE IT MAKES root1 == root2 !!!!
			verifyEditScript(root1, root2, editScript, reportItem);
			
			esa.report(reportItem, editScriptReportId);
			
			return editScript;
		}
		catch (Exception | Error e)
		{
			reportException(e);
			return null;
		}
	}
	
	private void verifyEditScript(
			DiffNode root1,
			DiffNode root2,
			List<EditOp> editScript,
			ReportItem reportItem) throws ComparisonException
	{
		boolean success = false;
		try
		{
			new EditScriptManager(editScript).apply();
			root1.compareNativeDeep(root2);
			success = true;
		}
		catch (Exception | Error e)
		{
			reportException(e);
		}
		finally
		{
			reportItem.recordFigure("30.01.a) Edit script verification", success ? 1 : 0, "bool");
		}
	}
	
	private boolean wikiDiffSplit(PageRevDiffNode prdn)
	{
		try
		{
			HDDiffOptions options = setupWikiDiff();
			options.setOnlySplitNodes(true);
			options.setAddSplitIds(true);
			
			DiffNode root1 = prdn.nodeA.getFirstChild();
			DiffNode root2 = prdn.nodeB.getFirstChild();
			
			HDDiff wd = new HDDiff(root1, root2, options, null);
			wd.editScript();
			return true;
		}
		catch (Exception | Error e)
		{
			return false;
		}
	}
	
	public HDDiffOptions setupWikiDiff()
	{
		HDDiffOptions options = new HDDiffOptions();
		
		options.setNodeMetrics(new WomNodeMetrics());
		
		options.setMinSubtreeWeight(12);
		
		options.setEnableTnsm(true);
		options.setTnsmEligibilityTester(new WomNodeEligibilityTester());
		options.setTnsmSubstringJudge(new WordSubstringJudge(8, 3));
		
		return options;
	}
	
	// =========================================================================
	
	private boolean myersDiff(
			PageRevText prt,
			ReportItem reportItem,
			String timerId,
			String reportId)
	{
		try
		{
			DiffMatchPatch dfp = new DiffMatchPatch();
			
			Timer timer = reportItem.startTimer(timerId);
			LinkedList<Diff> diff = dfp.diff_main(prt.textA, prt.textB, false);
			timer.stop();
			
			EditScriptAnalysis esa = new EditScriptAnalysis(diff);
			esa.report(reportItem, reportId);
			
			return true;
		}
		catch (Exception | Error e)
		{
			reportException(e);
			return false;
		}
	}
	
	// =========================================================================
	
	private boolean xyDiff(
			PageRevDiffNode prdn,
			File compactWomA,
			File compactWomB,
			ReportItem reportItem,
			String timerId,
			String reportId)
	{
		try
		{
			RunXyDiff xyd = new RunXyDiff();
			
			Timer timer = reportItem.startTimer(timerId);
			EditScriptAnalysis esa = xyd.run(prdn, compactWomA, compactWomB);
			timer.stop();
			
			esa.report(reportItem, reportId);
			return true;
		}
		catch (XyDiffException e)
		{
			if (e.getCause() instanceof TimeoutException)
			{
				reportOnStdErr("XyDiff timeout");
			}
			else
			{
				reportException(e);
			}
		}
		catch (Exception | Error e)
		{
			reportException(e);
		}
		
		return false;
	}
	
	// =========================================================================
	
	private boolean fcDiff(
			PageRevDiffNode prdn,
			File compactWomA,
			File compactWomB,
			ReportItem reportItem,
			String timerId,
			String reportId)
	{
		try
		{
			RunFcDiff fcd = new RunFcDiff();
			
			Timer timer = reportItem.startTimer(timerId);
			EditScriptAnalysis esa = fcd.run(prdn, compactWomA, compactWomB);
			timer.stop();
			
			esa.report(reportItem, reportId);
			return true;
		}
		catch (FcDiffException e)
		{
			if ("FcDiff output syntax confuses me!".equals(e.getMessage()))
			{
				reportOnStdErr("FcDiff confusion");
			}
			else if (e.getCause() instanceof ComparisonException)
			{
				reportOnStdErr("FcDiff confusion");
			}
			else
			{
				reportException(e);
			}
		}
		catch (Exception | Error e)
		{
			reportException(e);
		}
		
		return false;
	}
	
	// =========================================================================
	
	private static void reportOnStdErr(String msg)
	{
		System.out.println();
		System.out.flush();
		System.err.println(msg);
		System.err.flush();
	}
	
	private static void reportException(Throwable e)
	{
		System.out.println();
		System.out.flush();
		e.printStackTrace();
		System.err.flush();
	}
}
