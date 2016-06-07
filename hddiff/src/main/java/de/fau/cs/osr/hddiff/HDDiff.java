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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.fau.cs.osr.hddiff.HDDiffOptions.TreeDumpPhases;
import de.fau.cs.osr.hddiff.editscript.EditOp;
import de.fau.cs.osr.hddiff.editscript.EditOpDelete;
import de.fau.cs.osr.hddiff.editscript.EditOpInsert;
import de.fau.cs.osr.hddiff.editscript.EditOpMove;
import de.fau.cs.osr.hddiff.editscript.EditOpSplit;
import de.fau.cs.osr.hddiff.editscript.EditOpUpdate;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.tree.NodeEligibilityTesterInterface;
import de.fau.cs.osr.hddiff.tree.NodeMetricsInterface;
import de.fau.cs.osr.hddiff.utils.ElementComparatorInterface;
import de.fau.cs.osr.hddiff.utils.HDDiffTreeVisualizer;
import de.fau.cs.osr.hddiff.utils.LcsMyers;
import de.fau.cs.osr.hddiff.utils.NOCSstr;
import de.fau.cs.osr.hddiff.utils.NOCSstr.CommonSubstring;
import de.fau.cs.osr.hddiff.utils.ReportItem;
import de.fau.cs.osr.hddiff.utils.ReportItem.Timer;
import de.fau.cs.osr.utils.ComparisonException;

public class HDDiff
{
	private static final int NODE_COUNT_ASSUMPTION = 1000;

	private static final Object DUPLICATE_INDICATOR_1 = new Object();

	private static final SubtreeMatch DUPLICATE_INDICATOR_2 = new SubtreeMatch(null, null, Integer.MIN_VALUE);

	private static final boolean ASSERTIONS = true;

	// =========================================================================

	public static List<EditOp> editScript(
			DiffNode root1,
			DiffNode root2,
			HDDiffOptions options)
	{
		return new HDDiff(root1, root2, options, null).editScript();
	}

	public static List<EditOp> editScript(
			DiffNode root1,
			DiffNode root2,
			HDDiffOptions options,
			ReportItem report)
	{
		return new HDDiff(root1, root2, options, report).editScript();
	}

	// =========================================================================

	private final DiffNode root1;

	private final DiffNode root2;

	private final HDDiffOptions options;

	private final ReportItem report;

	private final NodeMetricsInterface nodeMetrics;

	private final int minSubtreeWeight;

	// =========================================================================
	// Precomputation & Subtree matching

	private boolean precomputeT1;

	private Map<Integer, Object> subtreeHashes1;

	private Map<Integer, SubtreeMatch> subtreeHashes2;

	// =========================================================================
	// STATS: Precomputation & Subtree matching

	private int nodeCount1;

	private int nodeCount2;

	private int leafCount1;

	private int leafCount2;

	private int textLength1;

	private int textLength2;

	private int ssrSuitableSubtreeCount1;

	private int ssrSuitableSubtreeCount2;

	private int ssrSubtreeMatchCount;

	private int ssrSubtreeNodeMatchCount;

	// =========================================================================
	// TSNM matching

	private ArrayList<DiffNode> leafSeq1;

	private ArrayList<DiffNode> leafSeq2;

	// DEBUG
	private int splitMatchId = 0;

	// =========================================================================
	// STATS: TSNM matching

	private int nocssNodeMatchCount;

	private int nocssNodeSplitCount;

	// =========================================================================
	// Bottom Up Top Down pass

	private final Candidate searchCandidate = new Candidate(null, null, -1);

	private Map<Candidate, Candidate> ancestorCandidates;

	// =========================================================================
	// STATS: Bottom Up Top Down pass

	private int bottomUpAncestorMatchCount;

	private LcsMyers<DiffNode> pathTypeLcs;

	// =========================================================================
	// Top Down pass

	private ArrayList<DiffNode> siblingSeq1;

	private ArrayList<DiffNode> siblingSeq2;

	private LcsMyers<DiffNode> childHashLcs;

	private LcsMyers<DiffNode> siblingTypeLcs;

	// =========================================================================
	// STATS: Top Down pass

	private int t2ttdSubtreeMatchCount;

	private int t2ttdSubtreeNodeMatchCount;

	private int t2ttdChildLabelNodeMatchCount;

	private int editScriptUpdateCount;

	private int editScriptMoveCount;

	private int editScriptInsertCount;

	private int editScriptAlignmentCount;

	private int editScriptDeleteCount;

	// =========================================================================

	private LinkedList<EditOp> editScript;

	// =========================================================================

	public HDDiff(
			DiffNode root1,
			DiffNode root2,
			HDDiffOptions options,
			ReportItem report)
	{
		if (!root1.isSameNodeType(root2))
			throw new IllegalArgumentException("We assume that the root "
					+ "nodes always match. Therefore, the root nodes must be "
					+ "of the same kind!");

		this.root1 = root1;
		this.root2 = root2;
		this.options = options;
		this.nodeMetrics = options.getNodeMetrics();
		this.minSubtreeWeight = options.getMinSubtreeWeight();
		this.report = report;
	}

	// =========================================================================

	public List<EditOp> editScript()
	{
		Timer timer = null;
		if (report != null)
			timer = report.startTimer("00) HDDiff");

		editScript = new LinkedList<>();

		try
		{
			precompute();

			if (options.getDumpTreesPhase() == TreeDumpPhases.AFTER_PRECOMPUTE)
				HDDiffTreeVisualizer.drawGraph(options, root1, root2);

			boolean complete = false;
			complete = greedySubtreeMatching();

			if (complete)
			{
				// Also checks for updates
				ssrMapSubtrees(root1, root2);
			}

			if (options.getDumpTreesPhase() == TreeDumpPhases.AFTER_SSR)
				HDDiffTreeVisualizer.drawGraph(options, root1, root2);

			if (!complete)
			{
				/**
				 * Make sure the root nodes always match. We assured in the
				 * constructor that the root nodes have the same type. Their
				 * properties/values might still differ, in which case an update
				 * operation will be added to the edit script laters.
				 */
				mapFull(root1, root2);

				if (options.isTnsmEnabled())
					textNodeSplitMatching();

				if (options.getDumpTreesPhase() == TreeDumpPhases.AFTER_TNSM)
					HDDiffTreeVisualizer.drawGraph(options, root1, root2);

				if (!options.isOnlySplitNodes())
				{
					bottomUpMatching();

					if (options.getDumpTreesPhase() == TreeDumpPhases.AFTER_TREE2TREE_BOTTOMUP)
						HDDiffTreeVisualizer.drawGraph(options, root1, root2);

					// Also builds edit script
					topDownMatching();

					if (options.getDumpTreesPhase() == TreeDumpPhases.AFTER_TREE2TREE_TOPDOWN)
						HDDiffTreeVisualizer.drawGraph(options, root1, root2);

					gatherDeletes();
				}
			}
		}
		finally
		{
			if (timer != null)
				timer.stop();
		}

		return editScript;
	}

	// =========================================================================

	private void precompute()
	{
		precomputationWalk();
	}

	private void precomputationWalk()
	{
		Timer timer = null;
		if (report != null)
			timer = report.startTimer("00.01) Precomputation");

		try
		{
			// Do T1
			{
				leafSeq1 = new ArrayList<>(NODE_COUNT_ASSUMPTION);
				subtreeHashes1 = new HashMap<>(NODE_COUNT_ASSUMPTION);

				precomputeT1 = true;
				precompute(root1);

				if (report != null)
				{
					report.recordFigure("00.01.a) PRECOMP: Nodes in T1", nodeCount1, "#");
					report.recordFigure("00.01.b) PRECOMP: Leaves in T1", leafCount1, "#");
					report.recordFigure("00.01.c) PRECOMP: Inner nodes in T1", nodeCount1 - leafCount1, "#");
					report.recordFigure("00.01.d) PRECOMP: Text length in T1", textLength1, "#");
				}
			}

			// Do T2
			{
				/**
				 * 1.2f is an arbitrary guess that was never tested... It simply
				 * makes sure that the initial capacity is sufficient if the
				 * document grew slightly in size.
				 */
				int initialCapacity = (int) (nodeCount1 * 1.2f);
				leafSeq2 = new ArrayList<>(initialCapacity);
				subtreeHashes2 = new HashMap<>(initialCapacity);

				// Make sure subtreeHashes1 is not written for T2
				Map<Integer, Object> subtreeHashes1tmp = subtreeHashes1;
				subtreeHashes1 = null;

				precomputeT1 = false;
				precompute(root2);

				if (report != null)
				{
					report.recordFigure("00.01.a) PRECOMP: Nodes in T2", nodeCount2, "#");
					report.recordFigure("00.01.b) PRECOMP: Leaves in T2", leafCount2, "#");
					report.recordFigure("00.01.c) PRECOMP: Inner nodes in T2", nodeCount2 - leafCount2, "#");
					report.recordFigure("00.01.d) PRECOMP: Text length in T2", textLength2, "#");
				}

				subtreeHashes1 = subtreeHashes1tmp;
			}
		}
		finally
		{
			if (timer != null)
				timer.stop();
		}
	}

	private void precompute(DiffNode node)
	{
		DiffNode child = node.getFirstChild();
		if (child != null)
		{
			// node is a inner node
			do
			{
				precompute(child);
				child = child.getNextSibling();
			} while (child != null);
		}
		else
		{
			// node is a leaf node
			if (leafSeq2 != null)
				leafSeq2.add(node);
			else if (leafSeq1 != null)
				leafSeq1.add(node);

			int textSize = 0;
			if (node.isTextLeaf())
				textSize = node.getTextContent().length();

			if (precomputeT1)
			{
				textLength1 += textSize;
				++leafCount1;
			}
			else
			{
				textLength2 += textSize;
				++leafCount2;
			}
		}

		// post-order visit

		DiffNode parent = node.getParent();

		int newWeight = node.addWeight(nodeMetrics.computeWeight(node));
		int newHash = node.updateSubtreeHash(nodeMetrics.computeHash(node));

		if (parent != null)
		{
			parent.addWeight(newWeight);
			parent.updateSubtreeHash(31 * newHash);
		}

		if ((subtreeHashes1 != null) &&
				(newWeight >= minSubtreeWeight))
		{
			Object old = subtreeHashes1.put(newHash, node);
			++ssrSuitableSubtreeCount1;

			/**
			 * There's a possibility of hash collisions here. But checking the
			 * suspected subtrees for real equality is too expensive. It would
			 * be done for duplicate subtrees as well as for hash collisions. We
			 * assume that hash collisions are far more unlikely then duplicate
			 * trees and accept that we might accidently flag a subtree as
			 * duplicate although it isn't. This decreases (initial) match
			 * quality but does not affect correctness.
			 */
			if (old != null)
			{
				if (isWarningEnabled())
					report.warn("00.01) SSR: Subtree hash collision in T1!");

				subtreeHashes1.put(newHash, DUPLICATE_INDICATOR_1);
			}
		}

		if (precomputeT1)
			++nodeCount1;
		else
			++nodeCount2;
	}

	// =========================================================================

	private boolean greedySubtreeMatching()
	{
		Timer timer = null;
		if (report != null)
			timer = report.startTimer("00.02) Greedy subtree matching");

		try
		{
			{
				Timer timer2 = null;
				if (report != null)
					timer2 = report.startTimer("00.02.01) SSR: Matching subtrees in T2");

				try
				{
					if (matchSubtreesInT2(root2))
						// The tree's structure is identical
						return true;
				}
				finally
				{
					if (timer2 != null)
						timer2.stop();
				}
			}

			{
				Timer timer2 = null;
				if (report != null)
					timer2 = report.startTimer("00.02.02) SSR: Mapping matched subtrees");

				try
				{
					SubtreeMatch[] n2sorted = subtreeHashes2.values().toArray(
							new SubtreeMatch[subtreeHashes2.size()]);

					Arrays.sort(n2sorted);
					mapAllSubtrees(n2sorted);
					return false;
				}
				finally
				{
					if (timer2 != null)
						timer2.stop();
				}
			}
		}
		finally
		{
			// Now garbage
			subtreeHashes1 = null;
			subtreeHashes2 = null;

			if (timer != null)
				timer.stop();
		}
	}

	private boolean matchSubtreesInT2(DiffNode n2)
	{
		// No point in checking for match or descending further if weight of 
		// this node is not sufficient.
		int weight = n2.getWeight();
		if (weight >= minSubtreeWeight)
		{
			boolean descend;

			++ssrSuitableSubtreeCount2;

			int hash = n2.getSubtreeHash();
			Object partnerObj = subtreeHashes1.get(hash);
			if ((partnerObj != null) && (partnerObj != DUPLICATE_INDICATOR_1))
			{
				DiffNode n1 = (DiffNode) partnerObj;

				if ((n1 == root1) || (n2 == root2))
				{
					boolean identical = ((n1 == root1) && (n2 == root2));
					if (identical)
						return true;

					/**
					 * We cannot match one root to anything else but the other
					 * root. If a root matches some subtree we have to ignore
					 * this match and continue.
					 */
					descend = true;
				}
				else
				{
					SubtreeMatch old2 = subtreeHashes2.put(
							hash,
							new SubtreeMatch(n1, n2, weight));

					/**
					 * There's a possibility of hash collisions here. But
					 * checking the suspected subtrees for real equality is too
					 * expensive. It would be done for duplicate subtrees as
					 * well as for hash collisions. We assume that hash
					 * collisions are far more unlikely then duplicate trees and
					 * accept that we might accidently flag a subtree as
					 * duplicate although it isn't. This decreases (initial)
					 * match quality but does not affect correctness.
					 */
					if ((old2 != null) || (old2 == DUPLICATE_INDICATOR_2))
					{
						if (isWarningEnabled())
							report.warn("00.02.01) SSR: Subtree hash collision in T2!");

						subtreeHashes2.put(hash, DUPLICATE_INDICATOR_2);
					}

					/**
					 * Something matched, either a duplicate or a real match. In
					 * neither case should we descend. Either all children are
					 * duplicates as well or all children are matches. Only
					 * exception is if the hash matched yet the trees were
					 * really different. This impacts quality but not
					 * correctness. We accept it for performance reasons.
					 */
					descend = false;
				}
			}
			else
				descend = true;

			if (descend)
			{
				for (DiffNode c2 = n2.getFirstChild(); c2 != null; c2 = c2.getNextSibling())
					matchSubtreesInT2(c2);
			}
		}

		return false;
	}

	private void mapAllSubtrees(SubtreeMatch[] n2sorted)
	{
		for (int i = 0; i < n2sorted.length; ++i)
		{
			SubtreeMatch match = n2sorted[i];
			if (match == DUPLICATE_INDICATOR_2)
				// Due to the sorting only duplicates can follow.
				break;

			DiffNode n1 = match.n1;
			DiffNode n2 = match.n2;

			// Subtree is subtree of an already matched subtree.
			// TODO: Can that even happen?
			if (isMatched(n1) || isMatched(n2))
				continue;

			ssrMapSubtrees(n1, n2);
		}

		if (report != null)
		{
			report.recordFigure("00.02.a) SSR: Suitable subtrees in T1", ssrSuitableSubtreeCount1, "#");
			report.recordFigure("00.02.b) SSR: Suitable subtrees in T2", ssrSuitableSubtreeCount2, "#");
			report.recordFigure("00.02.c) SSR: subtrees matched", ssrSubtreeMatchCount, "#");
			report.recordFigure("00.02.c) SSR: nodes matched", ssrSubtreeNodeMatchCount, "#");
		}
	}

	private void ssrMapSubtrees(DiffNode n1, DiffNode n2)
	{
		int nodesMatched = mapSubtrees(n1, n2);
		if (nodesMatched > 0)
		{
			ssrSubtreeNodeMatchCount += nodesMatched;
			++ssrSubtreeMatchCount;
		}
	}

	// =========================================================================

	private void textNodeSplitMatching()
	{
		Timer timer = null;
		if (report != null)
			timer = report.startTimer("00.03) Text node splitting & matching");

		try
		{
			NodeEligibilityTesterInterface tester = options.getTnsmEligibilityTester();

			String str1;
			String str2;
			ArrayList<NodeCharPos> nodeMap1 = new ArrayList<>(textLength1);
			ArrayList<NodeCharPos> nodeMap2 = new ArrayList<>(textLength2);
			{
				Timer timer2 = null;
				if (report != null)
					timer2 = report.startTimer("00.03.01) TNSM: Building leaf strings");

				try
				{
					str1 = unmatchedLeafString(true, tester, leafSeq1, nodeMap1);
					if (nodeMap1.isEmpty() || str1.isEmpty())
						return;

					str2 = unmatchedLeafString(false, tester, leafSeq2, nodeMap2);
					if (nodeMap2.isEmpty() || str2.isEmpty())
						return;
				}
				finally
				{
					if (timer2 != null)
						timer2.stop();
				}
			}

			if (report != null)
			{
				report.recordFigure("00.03.01.a) TNSM: Leaf string length in T1", str1.length(), "#chr");
				report.recordFigure("00.03.01.b) TNSM: Leaf string length in T2", str2.length(), "#chr");
			}

			List<CommonSubstring> nocss;
			{
				Timer timer2 = null;
				if (report != null)
					timer2 = report.startTimer("00.03.02) TNSM: NOCSS computation");

				try
				{
					nocss = NOCSstr.compute(
							str1,
							str2,
							NOCSstr.MARKER_SEQ_D_MIN + 1,
							options.getTnsmSubstringJudge(),
							null/*report*/);
				}
				finally
				{
					if (timer2 != null)
						timer2.stop();
				}
			}

			if (report != null)
				report.recordFigure("00.03.02.a) TNSM: Number of NOCSS", nocss.size(), "#");

			if (!nocss.isEmpty())
			{
				Timer timer2 = null;
				if (report != null)
					timer2 = report.startTimer("00.03.03) TNSM: Node splitting");

				try
				{
					int nocssCount = nocss.size();
					int maxNocsLen = nocss.get(0).len;
					int minNocsLen = nocss.get(nocssCount - 1).len;
					if (report != null)
					{
						report.recordFigure("00.03.03.a) TNSM: Max NOCS length", maxNocsLen, "#chr");
						report.recordFigure("00.03.03.c) TNSM: Min NOCS length", minNocsLen, "#chr");
					}

					for (CommonSubstring nocs : nocss)
						splitMatchedTexts(str1, str2, nodeMap1, nodeMap2, nocs);

					if (report != null)
					{
						report.recordFigure("00.03.a) TNSM: nodes matched", nocssNodeMatchCount, "#");
						report.recordFigure("00.03.b) TNSM: nodes splitted", nocssNodeSplitCount, "#");
					}
				}
				finally
				{
					if (timer2 != null)
						timer2.stop();
				}
			}
		}
		finally
		{
			leafSeq1 = null;
			leafSeq2 = null;

			if (timer != null)
				timer.stop();
		}
	}

	private String unmatchedLeafString(
			boolean left,
			NodeEligibilityTesterInterface tester,
			ArrayList<DiffNode> seq,
			ArrayList<NodeCharPos> nodeCharPos)
	{
		int nodeCounter = 0;

		boolean hadSep = false;
		StringBuilder sb = new StringBuilder(left ? textLength1 : textLength2);
		for (DiffNode node : seq)
		{
			if (!isMatched(node))
			{
				if (tester.isEligible(node))
				{
					++nodeCounter;
					String textContent = node.getTextContent();
					if (textContent != null)
					{
						sb.append(textContent);
						for (int i = 0; i < textContent.length(); ++i)
							nodeCharPos.add(new NodeCharPos(node, i));
						hadSep = false;
					}
				}
			}
			else
			{
				if (!hadSep)
				{
					sb.append((char) NOCSstr.MARKER_SEQ_D_MIN);
					nodeCharPos.add(new NodeCharPos(node, -1));
					hadSep = true;
				}
			}
		}

		String result = sb.toString();

		if (isDebugEnabled())
			report.debug(
					"00.03.01) TNSM: Created leaf string from %d nodes of length %d: '%s'",
					nodeCounter,
					result.length(),
					abbreviateRep(result));

		return result;
	}

	private void splitMatchedTexts(
			String str1,
			String str2,
			ArrayList<NodeCharPos> nodeMap1,
			ArrayList<NodeCharPos> nodeMap2,
			CommonSubstring nocs)
	{
		boolean addSplitIds = options.isAddSplitIds();
		boolean recordSplitOps = options.isRecordSplitOps();

		int start1 = nocs.start1;
		int start2 = nocs.start2;
		int len = nocs.len;

		int splitCounter = 0;
		int matchCounter = 0;

		NodeCharPos lastNcp1 = (start1 > 0) ? nodeMap1.get(start1 - 1) : null;
		NodeCharPos lastNcp2 = (start2 > 0) ? nodeMap2.get(start2 - 1) : null;

		NodeCharPos ncp1 = nodeMap1.get(start1);
		NodeCharPos ncp2 = nodeMap2.get(start2);

		DiffNode curNode1 = ncp1.node;
		int curNode1PosCorrect = 0;

		// We have to split if we don't start at node boundary
		DiffNode startSplit1 = null;
		if ((lastNcp1 != null) && (lastNcp1.node == ncp1.node))
		{
			startSplit1 = curNode1 = splitText(ncp1.node, ncp1.pos, recordSplitOps);
			ncp1.node.setWeight(nodeMetrics.computeWeight(ncp1.node));
			ncp1.node.setSplit(true);
			curNode1PosCorrect = ncp1.pos;
			++splitCounter;
			++nocssNodeSplitCount;
		}

		DiffNode curNode2 = ncp2.node;
		int curNode2PosCorrect = 0;

		DiffNode startSplit2 = null;
		if ((lastNcp2 != null) && (lastNcp2.node == ncp2.node))
		{
			startSplit2 = curNode2 = splitText(ncp2.node, ncp2.pos, false/*recordSplitOps*/);
			ncp2.node.setWeight(nodeMetrics.computeWeight(ncp2.node));
			ncp2.node.setSplit(true);
			curNode2PosCorrect = ncp2.pos;
			++splitCounter;
			++nocssNodeSplitCount;
		}

		for (int i = 1; i <= len; ++i)
		{
			lastNcp1 = ncp1;
			lastNcp2 = ncp2;

			// Binary search for break
			i = binarySearchBreak(nodeMap1, nodeMap2, lastNcp1, lastNcp2, start1, start2, i, len);

			if (addSplitIds)
			{
				curNode1.setNativeId("MATCH-" + splitMatchId);
				curNode2.setNativeId("MATCH-" + splitMatchId);
				++splitMatchId;
			}

			boolean break1;
			boolean break2;
			boolean endNocs = (i >= len);
			if (!endNocs)
			{
				ncp1 = nodeMap1.get(start1 + i);
				ncp2 = nodeMap2.get(start2 + i);
				// Means: node in T1 breaks
				break1 = lastNcp1.node != ncp1.node;
				break2 = lastNcp2.node != ncp2.node;
			}
			else
			{
				ncp1 = (start1 + i < nodeMap1.size()) ? nodeMap1.get(start1 + i) : null;
				ncp2 = (start2 + i < nodeMap2.size()) ? nodeMap2.get(start2 + i) : null;
				// Means: node in T1 continues but has to be split at NOCSstr end
				break1 = (ncp1 != null) && (lastNcp1.node == ncp1.node);
				break2 = (ncp2 != null) && (lastNcp2.node == ncp2.node);
			}

			DiffNode na1 = curNode1;
			DiffNode na2 = curNode2;

			/**
			 * Means: Node in T1 breaks so we have to split in T2 as well OR we
			 * are at the end of the NOCSstr and have to split T2 because the
			 * node would otherwise continue.
			 */
			if ((break1 && !break2 && !endNocs) || (break2 && endNocs))
			{
				curNode2 = splitText(na2, ncp2.pos - curNode2PosCorrect, false/*recordSplitOps*/);
				curNode2PosCorrect = ncp2.pos;
				++splitCounter;
				++nocssNodeSplitCount;

				if (break2)
					correctNodeMapForward(nodeMap2, start2 + i, ncp2.node, curNode2, curNode2PosCorrect);
			}

			if ((break2 && !break1 && !endNocs) || (break1 && endNocs))
			{
				curNode1 = splitText(na1, ncp1.pos - curNode1PosCorrect, recordSplitOps);
				curNode1PosCorrect = ncp1.pos;
				++splitCounter;
				++nocssNodeSplitCount;

				if (break1)
					correctNodeMapForward(nodeMap1, start1 + i, ncp1.node, curNode1, curNode1PosCorrect);
			}

			if (break1)
			{
				curNode1 = ncp1.node;
				curNode1PosCorrect = 0;
			}

			if (break2)
			{
				curNode2 = ncp2.node;
				curNode2PosCorrect = 0;
			}

			if ((break1 || break2) || endNocs)
			{
				int weight = nodeMetrics.computeWeight(na1);
				map(na1, na2, weight);
				++matchCounter;
				++nocssNodeMatchCount;

				curNode1.setSplit(true);
				if (curNode1 != na1)
				{
					na1.setSplit(true);
					na1.setWeight(nodeMetrics.computeWeight(na1));
					curNode1.setWeight(nodeMetrics.computeWeight(curNode1));
				}
				curNode2.setSplit(true);
				if (curNode2 != na2)
				{
					na2.setSplit(true);
					na2.setWeight(nodeMetrics.computeWeight(na2));
					curNode2.setWeight(nodeMetrics.computeWeight(curNode2));
				}
			}
		}

		if (startSplit1 != null)
			correctNodeMapBackward(nodeMap1, start1, startSplit1);
		if (startSplit2 != null)
			correctNodeMapBackward(nodeMap2, start2, startSplit2);

		if (isDebugEnabled())
			report.debug(
					"00.03.03) TNSM: NOCS = \"%s\", matches = %d, splits = %d",
					abbreviateRep(str1.substring(start1, start1 + len)),
					matchCounter,
					splitCounter);
	}

	private DiffNode splitText(DiffNode node, int pos, boolean recordSplitOps)
	{
		DiffNode otherHalf = node.splitText(pos);
		if (recordSplitOps)
			editScript.add(new EditOpSplit(node, pos, otherHalf));
		return otherHalf;
	}

	private int binarySearchBreak(
			ArrayList<NodeCharPos> nodeMap1,
			ArrayList<NodeCharPos> nodeMap2,
			NodeCharPos lastNcp1,
			NodeCharPos lastNcp2,
			int start1,
			int start2,
			int i,
			int len)
	{
		int k = len;
		int j = i;
		while (j < k)
		{
			int w = k - j;
			int x = j + w / 2;
			if ((nodeMap1.get(start1 + x).node == lastNcp1.node) &&
					(nodeMap2.get(start2 + x).node == lastNcp2.node))
			{
				if (w == 1)
					break;
				j = x;
			}
			else
			{
				k = x;
			}
		}
		i = k;
		return i;
	}

	private void correctNodeMapBackward(
			ArrayList<NodeCharPos> nodeMap,
			int i,
			DiffNode newNode)
	{
		/**
		 * We don't have to correct the whole node as we do in "forward". Only
		 * the first item to stop any attempts to break this node. The rest is
		 * inside the already matched substring and should never be touched by
		 * any other substring anyway.
		 */
		NodeCharPos ncp = nodeMap.get(i);
		ncp.node = newNode;
		ncp.pos = 0; // unnecessary
	}

	private void correctNodeMapForward(
			ArrayList<NodeCharPos> nodeMap,
			int i,
			DiffNode oldNode,
			DiffNode newNode,
			int posCorrect)
	{
		while (i < nodeMap.size())
		{
			NodeCharPos ncp = nodeMap.get(i);
			if (ncp.node != oldNode)
				break;
			ncp.node = newNode;
			ncp.pos -= posCorrect;
			++i;
		}
	}

	// =========================================================================

	private void bottomUpMatching()
	{
		Timer timer = null;
		if (report != null)
			timer = report.startTimer("00.04) Tree-to-tree bottom up (ancestor)");

		try
		{
			{
				Timer timer2 = null;
				if (report != null)
					timer2 = report.startTimer("00.04.01) T2T BU: Gather candidates");

				try
				{
					// Again: Quite arbitrary choice of initial size of paths arrays
					double depth = Math.log(nodeCount2) * 2;
					ArrayList<DiffNode> path1 = new ArrayList<>((int) depth);
					ArrayList<DiffNode> path2 = new ArrayList<>((int) depth);

					ancestorCandidates = new HashMap<>((nodeCount1 + nodeCount2) * 2);

					pathTypeLcs = new LcsMyers<>(new IsSameNodeTypeComparator());

					/**
					 * We have to skip the root node which has already been
					 * matched! This does not work if inner nodes are matched
					 * without matching the whole subtree!
					 */
					for (DiffNode child = root2.getFirstChild(); child != null; child = child.getNextSibling())
						gatherCandidates(child, path1, path2);
				}
				finally
				{
					if (timer2 != null)
						timer2.stop();
				}
			}

			{
				Timer timer2 = null;
				if (report != null)
					timer2 = report.startTimer("00.04.02) T2T BU: Candidate selection");

				try
				{
					Candidate[] sorted = ancestorCandidates.values().toArray(new Candidate[ancestorCandidates.size()]);
					Arrays.sort(sorted);

					for (int i = 0; i < sorted.length; ++i)
					{
						Candidate c = sorted[i];
						if (mapIfNotAlreadyMapped(c.n1, c.n2, c.common))
							++bottomUpAncestorMatchCount;
					}
				}
				finally
				{
					if (timer2 != null)
						timer2.stop();
				}
			}
		}
		finally
		{
			ancestorCandidates = null;
			pathTypeLcs = null;

			if (timer != null)
				timer.stop();
		}

		if (report != null)
			report.recordFigure("00.04.a) T2T BU: Ancestor nodes matched", bottomUpAncestorMatchCount, "#");
	}

	private void gatherCandidates(
			DiffNode node,
			ArrayList<DiffNode> path1,
			ArrayList<DiffNode> path2)
	{
		if (isMatched(node))
		{
			DiffNode partner = node.getPartner();
			fillPartnerPath(partner, path1);

			/*int d = */pathTypeLcs.lcs(path1, path2);
			ArrayList<DiffNode> s = pathTypeLcs.getLcs();
			for (int i = 0; i < s.size();)
			{
				DiffNode n1 = s.get(i++);
				DiffNode n2 = s.get(i++);
				addCandidate(n1, n2, partner.getWeight());
			}
		}
		else
		{
			// Unmatched inner node...
			DiffNode child = node.getFirstChild();
			if (child != null)
			{
				path2.add(node);

				for (; child != null; child = child.getNextSibling())
					gatherCandidates(child, path1, path2);

				removeLast(path2);
			}
			else
			{
				// Unmatched leaf...
			}
		}
	}

	private void addCandidate(DiffNode n1, DiffNode n2, int weight)
	{
		searchCandidate.n1 = n1;
		searchCandidate.n2 = n2;
		Candidate c = ancestorCandidates.get(searchCandidate);
		if (c != null)
		{
			c.common += weight;
		}
		else
		{
			c = new Candidate(n1, n2, weight);
			ancestorCandidates.put(c, c);
		}
	}

	private void fillPartnerPath(DiffNode partner, ArrayList<DiffNode> path1)
	{
		path1.clear();
		DiffNode node = partner;
		DiffNode parent = node.getParent();
		if (parent != null)
		{
			node = parent;
			parent = parent.getParent();
			while (parent != null)
			{
				path1.add(node);
				node = parent;
				parent = parent.getParent();
			}
		}
		Collections.reverse(path1);
	}

	private void removeLast(ArrayList<DiffNode> path)
	{
		path.remove(path.size() - 1);
	}

	// =========================================================================

	private void topDownMatching()
	{
		// TODO: Make configuration option.
		boolean matchByLabel = true;

		Timer timer2 = null;
		if (report != null)
			timer2 = report.startTimer("00.05) Tree-to-tree top down");

		try
		{
			siblingSeq1 = new ArrayList<>();
			siblingSeq2 = new ArrayList<>();

			childHashLcs = new LcsMyers<>(new IsSameSubtreeHashComparator(minSubtreeWeight));

			siblingTypeLcs = new LcsMyers<>(matchByLabel ?
					new IsMatchedOrSameNodeTypeComparator() :
					new IsPartnerComparator());

			t2ttdSubtreeMatchCount = 0;
			t2ttdSubtreeNodeMatchCount = 0;
			t2ttdChildLabelNodeMatchCount = 0;

			checkUpdate(root1, root2);
			topDownRec(root1, root2);

			if (report != null)
			{
				report.recordFigure("00.05.a) T2T TD: subtrees matched", t2ttdSubtreeMatchCount, "#");
				report.recordFigure("00.05.b) T2T TD: nodes matched by subtree", t2ttdSubtreeNodeMatchCount, "#");
				report.recordFigure("00.05.d) T2T TD: nodes matched by label", t2ttdChildLabelNodeMatchCount, "#");

				report.recordFigure("00.06.a) T2T ES: update count", editScriptUpdateCount, "#");
				report.recordFigure("00.06.b) T2T ES: move count", editScriptMoveCount, "#");
				report.recordFigure("00.06.c) T2T ES: insert count", editScriptInsertCount, "#");
				report.recordFigure("00.06.d) T2T ES: align count", editScriptAlignmentCount, "#");
			}
		}
		finally
		{
			siblingSeq1 = null;
			siblingSeq2 = null;
			childHashLcs = null;
			siblingTypeLcs = null;

			if (timer2 != null)
				timer2.stop();
		}
	}

	private void topDownRec(DiffNode n1, DiffNode n2)
	{
		if (n1 != n2.getPartner())
			throw new AssertionError("n1 != n2.getPartner()");

		if (n2.isLeaf())
			return;

		if (n1.isSubtreeMatched())
			return;

		// TODO: Make configuration option.
		boolean subtreeLcs = true;
		if (!n1.isLeaf())
		{
			if (subtreeLcs)
			{
				buildUnmatchedChildSeq(n1, siblingSeq1);
				buildUnmatchedChildSeq(n2, siblingSeq2);

				topDownSubtreeLcs();
			}

			buildCompleteChildSeq(n1, siblingSeq1);
			buildCompleteChildSeq(n2, siblingSeq2);

			topDownLabelLcs(n1, n2);
		}

		int i = 0;
		for (DiffNode c2 = n2.getFirstChild(); c2 != null; c2 = c2.getNextSibling())
		{
			DiffNode c1 = c2.getPartner();
			if (c1 == null)
			{
				c1 = c2.createSame(root1 /* for tree */);

				// TODO: Why is this necessary again?
				map(c1, c2, -2);

				if (ASSERTIONS)
				{
					if (i != c2.indexOf())
						throw new AssertionError("i != c2.indexOf()");
				}

				editScript.add(new EditOpInsert(
						n1,
						c1,
						c2,
						i));
				++editScriptInsertCount;
			}
			else
			{
				checkUpdate(c1, c2);

				DiffNode parent1 = c1.getParent();
				if (parent1 != n1)
				{
					if (ASSERTIONS)
					{
						if (i != c2.indexOf())
							throw new AssertionError("i != c2.indexOf()");
					}

					editScript.add(new EditOpMove(
							c1,
							n1,
							c2,
							i));
					++editScriptMoveCount;
				}
			}

			topDownRec(c1, c2);
			++i;
		}
	}

	// =========================================================================

	private void buildUnmatchedChildSeq(DiffNode n, ArrayList<DiffNode> seq)
	{
		seq.clear();
		for (DiffNode c = n.getFirstChild(); c != null; c = c.getNextSibling())
		{
			if (!isMatched(c))
				seq.add(c);
		}
	}

	private void topDownSubtreeLcs()
	{
		int s1len = siblingSeq1.size();
		int s2len = siblingSeq2.size();
		if ((s1len == 1) && (s2len == 1))
		{
			// Shortcut
			DiffNode c1 = siblingSeq1.get(0);
			DiffNode c2 = siblingSeq2.get(0);
			if (c1.getSubtreeHash() == c2.getSubtreeHash())
				mapSubtrees(c1, c2);

			return;
		}

		/*int d = */childHashLcs.lcs(siblingSeq1, siblingSeq2);

		ArrayList<DiffNode> s = childHashLcs.getLcs();
		for (int i = 0; i < s.size();)
		{
			DiffNode c1 = s.get(i++);
			DiffNode c2 = s.get(i++);
			topDownMapSubtrees(c1, c2);
		}
	}

	private void topDownMapSubtrees(DiffNode c1, DiffNode c2)
	{
		int nodesMatched = mapSubtrees(c1, c2);
		if (nodesMatched > 0)
		{
			t2ttdSubtreeNodeMatchCount += nodesMatched;
			++t2ttdSubtreeMatchCount;
		}
	}

	// =========================================================================

	private int mapSubtrees(DiffNode c1, DiffNode c2)
	{
		try
		{
			checkSubtreeEqualityDeep(c1, c2);
			c1.setSubtreeMatched(true);
			c2.setSubtreeMatched(true);
			return mapSubtreesRec(c1, c2);
		}
		catch (ComparisonException e)
		{
			if (isWarningEnabled())
				report.warn("00.04.03) T2T TD: Subtree mismatch (hash matched)!");
			return 0;
		}
	}

	private int mapSubtreesRec(DiffNode n1, DiffNode n2) throws ComparisonException
	{
		int nodeMatchCount = 0;

		checkUpdate(n1, n2);
		mapFull(n1, n2);
		++nodeMatchCount;

		DiffNode c1 = n1.getFirstChild();
		DiffNode c2 = n2.getFirstChild();
		while ((c1 != null) && (c2 != null))
		{
			nodeMatchCount += mapSubtreesRec(c1, c2);
			c1 = c1.getNextSibling();
			c2 = c2.getNextSibling();
		}

		return nodeMatchCount;
	}

	private void checkSubtreeEqualityDeep(DiffNode n1, DiffNode n2) throws ComparisonException
	{
		if (!nodeMetrics.verifyHashEquality(n1, n2))
			throw new ComparisonException();

		/**
		 * This can happen if during bottom-up a parent could not be matched.
		 * The parent might then get revisited by top-down and the LCS might try
		 * to match it with a duplicate in the other tree. However, in the other
		 * tree the matched node's parent WAS matched to some other node.
		 */
		DiffNode p1 = n1.getPartner();
		if ((p1 != null) && (p1 != n2))
			throw new ComparisonException();
		DiffNode p2 = n2.getPartner();
		if ((p2 != null) && (p2 != n1))
			throw new ComparisonException();

		DiffNode c1 = n1.getFirstChild();
		DiffNode c2 = n2.getFirstChild();
		while ((c1 != null) && (c2 != null))
		{
			checkSubtreeEqualityDeep(c1, c2);
			c1 = c1.getNextSibling();
			c2 = c2.getNextSibling();
		}

		if (c1 != c2)
			throw new ComparisonException();
	}

	// =========================================================================

	private void buildCompleteChildSeq(DiffNode n, ArrayList<DiffNode> seq)
	{
		seq.clear();
		for (DiffNode c = n.getFirstChild(); c != null; c = c.getNextSibling())
			seq.add(c);
	}

	private void topDownLabelLcs(DiffNode n1, DiffNode n2)
	{
		siblingTypeLcs.lcs(siblingSeq1, siblingSeq2);

		ArrayList<DiffNode> s = siblingTypeLcs.getLcs();

		int i = 0;
		for (int j = 0; j < siblingSeq2.size(); ++j)
		{
			DiffNode b = siblingSeq2.get(j);

			// An unaligned node must have a partner
			DiffNode a = b.getPartner();
			if (a != null)
			{
				// An unaligned node's partner must have the same parent
				DiffNode bParentsPartnerIn1 = b.getParent().getPartner();
				if (bParentsPartnerIn1 == a.getParent())
				{
					if ((i >= s.size()) || (b != s.get(i + 1)))
					{
						if (ASSERTIONS)
						{
							if (j != b.indexOf())
								throw new AssertionError("j != b.indexOf()");
						}

						editScript.add(new EditOpMove(
								a,
								n1,
								b,
								j));

						++editScriptAlignmentCount;
					}
					else
					{
						/**
						 * b has a partner and the partners have the same parent
						 * and b is part of the LCS. This means that b is
						 * properly aligned. We have to skip it.
						 */
						i += 2;
					}
				}
				else
				{
					/**
					 * b has a partner but they have different parents. It will
					 * be recorded as move soon. b cannot be part of the LCS now
					 * since its partner (which has a different parent) cannot
					 * be in siblingSeq2.
					 */
				}
			}
			else
			{
				/**
				 * b did not have a partner: it might be part of the LCS which
				 * means that it soon will have a partner. This also implies
				 * that the label matching will only create aligned matches.
				 */
				if ((i < s.size()) && (b == s.get(i + 1)))
				{
					DiffNode c1 = s.get(i++);
					DiffNode c2 = s.get(i++);

					if (c1.getPartner() != null)
						throw new AssertionError("c1.getPartner() != null");

					map(c1, c2, -3);
					++t2ttdChildLabelNodeMatchCount;
				}
				else
				{
					/**
					 * If b is not part of the LCS it means that b will become
					 * an insert.
					 */
				}
			}
		}
	}

	// =========================================================================

	private void checkUpdate(DiffNode n1, DiffNode n2)
	{
		if (!n1.isNodeValueEqual(n2))
		{
			editScript.add(new EditOpUpdate(
					n1,
					n2.getNodeValue(),
					n2));
			++editScriptUpdateCount;
		}
	}

	// =========================================================================

	private void gatherDeletes()
	{
		gatherDeletesRec(root1);

		if (report != null)
			report.recordFigure("00.06.e) T2T ES: delete count", editScriptDeleteCount, "#");
	}

	private void gatherDeletesRec(DiffNode n1)
	{
		for (DiffNode child = n1.getFirstChild(); child != null; child = child.getNextSibling())
			gatherDeletesRec(child);

		checkDelete(n1);
	}

	private void checkDelete(DiffNode n1)
	{
		if (n1.getPartner() == null)
		{
			editScript.add(new EditOpDelete(n1));
			++editScriptDeleteCount;
		}
	}

	// =========================================================================

	private void map(DiffNode nodeIn1, DiffNode nodeIn2, int common)
	{
		nodeIn1.set(nodeIn2, common);
		nodeIn2.set(nodeIn1, common);
	}

	private void mapFull(DiffNode n1, DiffNode n2)
	{
		n1.set(n2, n1.getWeight());
		n2.set(n1, n1.getWeight());
	}

	private boolean mapIfNotAlreadyMapped(DiffNode n1, DiffNode n2, int common)
	{
		if ((n1.getPartner() != null) || (n2.getPartner() != null))
			return false;
		n1.set(n2, common);
		n2.set(n1, common);
		return true;
	}

	private boolean isMatched(DiffNode node)
	{
		return (node.getPartner() != null);
	}

	private static String abbreviateRep(String label)
	{
		label = label.replace("\n", "\\n");
		label = label.replace("\"", "\\\"");
		label = StringUtils.abbreviateMiddle(label, "...", 64);
		return label;
	}

	private static String abbreviateRep(Object obj)
	{
		return abbreviateRep(String.valueOf(obj));
	}

	private boolean isDebugEnabled()
	{
		return (report != null) && (report.isDebugEnabled());
	}

	private boolean isWarningEnabled()
	{
		return (report != null) && (report.isWarningEnabled());
	}

	// =========================================================================

	private static final class Candidate
			implements
				Comparable<Candidate>
	{
		private DiffNode n1;

		private DiffNode n2;

		private int common;

		public Candidate(DiffNode node, DiffNode partner, int common)
		{
			this.n1 = node;
			this.n2 = partner;
			this.common = common;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + n1.hashCode();
			result = prime * result + n2.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			Candidate other = (Candidate) obj;
			return (other.n1 == this.n1) && (other.n2 == this.n2);
		}

		@Override
		public int compareTo(Candidate o)
		{
			// Sort DESCENDING
			return -Integer.compare(this.common, o.common);
		}

		@Override
		public String toString()
		{
			return "Candidate [node=" + abbreviateRep(n1) +
					", partner=" + abbreviateRep(n2) +
					", common=" + common + "]";
		}
	}

	// =========================================================================

	private static final class SubtreeMatch
			implements
				Comparable<SubtreeMatch>
	{
		private final DiffNode n1;

		private final DiffNode n2;

		private final int weight;

		public SubtreeMatch(DiffNode n1, DiffNode n2, int weight)
		{
			this.n1 = n1;
			this.n2 = n2;
			this.weight = weight;
		}

		@Override
		public int compareTo(SubtreeMatch o)
		{
			// Sort DESCENDING
			return -Integer.compare(weight, o.weight);
		}

		@Override
		public String toString()
		{
			return "SubtreeMatch [n1=" + abbreviateRep(n1) + ", n2=" + abbreviateRep(n2) + ", weight=" + weight + "]";
		}
	}

	// =========================================================================

	private static final class NodeCharPos
	{
		DiffNode node;

		int pos;

		NodeCharPos(DiffNode node, int pos)
		{
			super();
			this.node = node;
			this.pos = pos;
		}

		@Override
		public String toString()
		{
			return "NodeCharPos [node=" + abbreviateRep(node) + ", pos=" + pos + "]";
		}
	}

	// =========================================================================

	private static final class IsSameSubtreeHashComparator
			implements
				ElementComparatorInterface<DiffNode>
	{
		private final int minSubtreeWeight;

		public IsSameSubtreeHashComparator(int minSubtreeWeight)
		{
			this.minSubtreeWeight = minSubtreeWeight;
		}

		@Override
		public boolean equals(DiffNode a, DiffNode b)
		{
			return (a.getSubtreeHash() == b.getSubtreeHash()) && (a.getWeight() > minSubtreeWeight);
		}
	}

	private static final class IsSameNodeTypeComparator
			implements
				ElementComparatorInterface<DiffNode>
	{
		@Override
		public boolean equals(DiffNode a, DiffNode b)
		{
			return a.isSameNodeType(b);
		}
	}

	private static final class IsMatchedOrSameNodeTypeComparator
			implements
				ElementComparatorInterface<DiffNode>
	{
		@Override
		public boolean equals(DiffNode a, DiffNode b)
		{
			DiffNode aPartner = a.getPartner();
			return (aPartner == null) ?
					(b.getPartner() == null) && a.isSameNodeType(b) :
					(aPartner == b);
		}
	}

	private static final class IsPartnerComparator
			implements
				ElementComparatorInterface<DiffNode>
	{
		@Override
		public boolean equals(DiffNode a, DiffNode b)
		{
			return a.getPartner() == b;
		}
	}
}
