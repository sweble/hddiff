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
package de.fau.cs.osr.hddiff.perfsuite.editscript;

import java.util.LinkedList;
import java.util.List;

import de.fau.cs.osr.hddiff.editscript.EditOp;
import de.fau.cs.osr.hddiff.editscript.EditOpDelete;
import de.fau.cs.osr.hddiff.editscript.EditOpInsert;
import de.fau.cs.osr.hddiff.editscript.EditOpMove;
import de.fau.cs.osr.hddiff.editscript.EditOpUpdate;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.tree.NodeUpdate;
import de.fau.cs.osr.hddiff.utils.ReportItem;

/**
 * Implemented according to
 * 
 * <pre>
 * [1] Chawathe, Sudarshan S., et al.
 * "Change detection in hierarchically structured information."
 * ACM SIGMOD Record. Vol. 25. No. 2. ACM, 1996.
 * </pre>
 */
public class EditScriptBuilder
{
	public static List<EditOp> buildEditScript(
			DiffNode root1,
			DiffNode root2,
			ReportItem report)
	{
		return new EditScriptBuilder(root1, root2, report).buildEditScript();
	}
	
	// =========================================================================
	
	private final DiffNode root1;
	
	private final DiffNode root2;
	
	private final ReportItem report;
	
	// =========================================================================
	
	private EditScriptBuilder(
			DiffNode root1,
			DiffNode root2,
			ReportItem report)
	{
		if (!root1.isSameNodeType(root2))
		{
			throw new IllegalArgumentException("We assume that the root "
					+ "nodes always match. Therefore, the root nodes must be "
					+ "of the same kind!");
		}
		
		this.root1 = root1;
		this.root2 = root2;
		this.report = report;
	}
	
	// =========================================================================
	
	private List<EditOp> buildEditScript()
	{
		int insertions = 0;
		int deletions = 0;
		int moves = 0;
		int updates = 0;
		
		LinkedList<EditOp> editScript = new LinkedList<>();
		
		BreadthFirstTreeIterator iterRight = new BreadthFirstTreeIterator(root2);
		
		ChildrenAligner ca = new ChildrenAligner(editScript);
		
		while (iterRight.hasNext())
		{
			DiffNode nRight = iterRight.next();
			DiffNode nLeft = nRight.getPartner();
			
			DiffNode parentRight = nRight.getParent();
			
			DiffNode leftPartnerParentRight = null;
			if (parentRight != null)
				leftPartnerParentRight = parentRight.getPartner();
			
			if (nLeft == null)
			{
				DiffNode leftInsertedNode = nRight.createSame(root1 /* for tree */);
				
				editScript.add(new EditOpInsert(
						leftPartnerParentRight,
						leftInsertedNode,
						nRight,
						nRight.indexOf()));
				++insertions;
				
				// TODO: Can we avoid this?
				map(nRight, leftInsertedNode);
				
				// TODO: Can we avoid this?
				nLeft = leftInsertedNode;
			}
			else
			{
				updates += checkForUpdate(editScript, nRight, nLeft);
				
				DiffNode parentLeft = nLeft.getParent();
				
				if (leftPartnerParentRight != parentLeft)
				{
					editScript.add(new EditOpMove(
							nLeft,
							leftPartnerParentRight,
							nRight,
							nRight.indexOf()));
					++moves;
				}
			}
			
			// Children are aligned before we descend to them
			ca.alignChildren(nLeft, nRight);
		}
		
		deletions = collectDeletes(editScript, root1);
		
		moves += ca.getMoves();
		if (report != null)
		{
			report.recordFigure("00.09.a) ES: Edit script size", editScript.size(), "#ops");
			report.recordFigure("00.09.b) ES: Insertions", insertions, "#ops");
			report.recordFigure("00.09.c) ES: Deletions", deletions, "#ops");
			report.recordFigure("00.09.d) ES: Moves", moves, "#ops");
			report.recordFigure("00.09.e) ES: Updates", updates, "#ops");
		}
		
		return editScript;
	}
	
	private int checkForUpdate(
			LinkedList<EditOp> editScript,
			DiffNode nRight,
			DiffNode nLeft)
	{
		NodeUpdate update = nLeft.compareWith(nRight);
		if (update != null)
		{
			editScript.add(new EditOpUpdate(
					nLeft,
					update,
					nRight));
			return 1;
		}
		else
			return 0;
	}
	
	private int collectDeletes(
			LinkedList<EditOp> editScript,
			DiffNode nLeft)
	{
		int deletions = 0;
		
		for (DiffNode child = nLeft.getFirstChild(); child != null; child = child.getNextSibling())
			deletions += collectDeletes(editScript, child);
		
		if (nLeft.getPartner() == null)
		{
			editScript.add(new EditOpDelete(nLeft));
			++deletions;
		}
		
		return deletions;
	}
	
	// =========================================================================
	
	private void map(DiffNode nRight, DiffNode leftInsertedNode)
	{
		leftInsertedNode.set(nRight, Integer.MIN_VALUE);
		nRight.set(leftInsertedNode, Integer.MIN_VALUE);
	}
}
