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
package de.fau.cs.osr.hddiff;

import java.io.File;

import de.fau.cs.osr.hddiff.tree.NodeEligibilityTesterInterface;
import de.fau.cs.osr.hddiff.tree.NodeMetricsInterface;
import de.fau.cs.osr.hddiff.utils.SubstringJudgeInterface;

public final class HDDiffOptions
		implements
			Cloneable
{
	private NodeMetricsInterface nodeMetrics;

	// Debug stuff

	private TreeDumpPhases dumpTreesPhase;

	private String dumpTreesFileTitle;

	private File graphvizDotBin = new File("/usr/bin/dot");

	// Search Space Reduction (SSR) phase

	private int minSubtreeWeight;

	// Text node splitting & matching (TNSM) phase

	private boolean enableTnsm;

	private boolean recordSplitOps;

	private NodeEligibilityTesterInterface tnsmEligibilityTester;

	private SubstringJudgeInterface<String> tnsmSsj;

	// Debug stuff

	private boolean onlySplitNodes;

	private boolean addSplitIds;

	// =========================================================================

	public HDDiffOptions()
	{
	}

	// =========================================================================
	// Debug stuff

	public void setDumpTreesPhase(TreeDumpPhases dumpTreesPhase)
	{
		this.dumpTreesPhase = dumpTreesPhase;
	}

	public TreeDumpPhases getDumpTreesPhase()
	{
		return dumpTreesPhase;
	}

	public void setDumpTreesFileTitle(String dumpTressFileTitle)
	{
		this.dumpTreesFileTitle = dumpTressFileTitle;
	}

	public String getDumpTreesFileTitle()
	{
		return dumpTreesFileTitle;
	}

	public void setGraphvizDotBin(File graphvizDotBin)
	{
		this.graphvizDotBin = graphvizDotBin;
	}

	public File getGraphvizDotBin()
	{
		return graphvizDotBin;
	}

	// =========================================================================
	// Debug stuff

	public boolean isOnlySplitNodes()
	{
		return onlySplitNodes;
	}

	public void setOnlySplitNodes(boolean onlySplitNodes)
	{
		this.onlySplitNodes = onlySplitNodes;
	}

	public boolean isAddSplitIds()
	{
		return addSplitIds;
	}

	public void setAddSplitIds(boolean addSplitIds)
	{
		this.addSplitIds = addSplitIds;
	}

	// =========================================================================
	// General stuff

	/**
	 * Used to query various information about a node like hash or weight.
	 */
	public void setNodeMetrics(NodeMetricsInterface nodeMetrics)
	{
		this.nodeMetrics = nodeMetrics;
	}

	public NodeMetricsInterface getNodeMetrics()
	{
		return nodeMetrics;
	}

	// =========================================================================
	// Search Space Reduction (SSR) phase

	public void setMinSubtreeWeight(int minSubtreeWeight)
	{
		this.minSubtreeWeight = minSubtreeWeight;
	}

	public int getMinSubtreeWeight()
	{
		return minSubtreeWeight;
	}

	// =========================================================================
	// Text node splitting & matching (TNSM) phase

	public void setEnableTnsm(boolean enableTnsm)
	{
		this.enableTnsm = enableTnsm;
	}

	public boolean isTnsmEnabled()
	{
		return enableTnsm;
	}

	public void setRecordSplitOps(boolean recordSplitOps)
	{
		this.recordSplitOps = recordSplitOps;
	}

	public boolean isRecordSplitOps()
	{
		return recordSplitOps;
	}

	public void setTnsmEligibilityTester(
			NodeEligibilityTesterInterface tnsmEligibilityTester)
	{
		this.tnsmEligibilityTester = tnsmEligibilityTester;
	}

	public NodeEligibilityTesterInterface getTnsmEligibilityTester()
	{
		return tnsmEligibilityTester;
	}

	public void setTnsmSubstringJudge(
			SubstringJudgeInterface<String> tnsmSsj)
	{
		this.tnsmSsj = tnsmSsj;
	}

	public SubstringJudgeInterface<String> getTnsmSubstringJudge()
	{
		return tnsmSsj;
	}

	// =========================================================================

	public enum TreeDumpPhases
	{
		AFTER_PRECOMPUTE,
		AFTER_SSR,
		AFTER_TNSM,
		AFTER_TREE2TREE_BOTTOMUP,
		AFTER_TREE2TREE_TOPDOWN,
		SKIP,
	}
}
