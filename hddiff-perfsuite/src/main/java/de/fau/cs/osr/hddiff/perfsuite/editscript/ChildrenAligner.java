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

import java.util.ArrayList;
import java.util.LinkedList;

import de.fau.cs.osr.hddiff.editscript.EditOp;
import de.fau.cs.osr.hddiff.editscript.EditOpMove;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.utils.ElementComparatorInterface;
import de.fau.cs.osr.hddiff.utils.LcsMyers;

public class ChildrenAligner
{
	private final LinkedList<EditOp> editScript;
	
	private final ArrayList<DiffNode> s1 = new ArrayList<>();
	
	private final ArrayList<DiffNode> s2 = new ArrayList<>();
	
	private final LcsMyers<DiffNode> lcs = new LcsMyers<>(new MutualPartnerComparator());
	
	private int moves;
	
	// =========================================================================
	
	public ChildrenAligner(LinkedList<EditOp> editScript)
	{
		this.editScript = editScript;
	}
	
	// =========================================================================
	
	public int getMoves()
	{
		return moves;
	}
	
	// =========================================================================
	
	public void alignChildren(DiffNode nLeft, DiffNode nRight)
	{
		if (getChildSeqs(nLeft, nRight))
			// Everything is in order...
			return;
		
		/*int d= */lcs.lcs(s1, s2);
		
		ArrayList<DiffNode> s = lcs.getLcs();
		
		if (s.isEmpty() || (s.size() == 2 * Math.min(s1.size(), s2.size())))
			/**
			 * If the size of the intersection is the same as the size of one of
			 * the children lists then only nodes were added/deleted and no
			 * moves have taken place -> all existing nodes are in order.
			 * 
			 * If the intersection is empty, no alignments have to be made.
			 */
			return;
		
		generateAlignmentOperations(nLeft, s);
	}
	
	private boolean getChildSeqs(DiffNode nLeft, DiffNode nRight)
	{
		s1.clear();
		s2.clear();
		
		DiffNode l = nLeft.getFirstChild();
		DiffNode r = nRight.getFirstChild();
		
		boolean allInOrder = true;
		while (l != null && r != null)
		{
			if (allInOrder && !equals(l, r))
				allInOrder = false;
			
			s1.add(l);
			s2.add(r);
			
			l = l.getNextSibling();
			r = r.getNextSibling();
		}
		
		if (l != r)
			// Both have to be null for equality
			allInOrder = false;
		
		for (; l != null; l = l.getNextSibling())
			s1.add(l);
		for (; r != null; r = r.getNextSibling())
			s2.add(r);
		
		return allInOrder;
	}
	
	private void generateAlignmentOperations(
			DiffNode nLeft,
			ArrayList<DiffNode> s)
	{
		int i = 0;
		for (DiffNode a : s1)
		{
			// An unaligned node must have a partner
			DiffNode b = a.getPartner();
			if (b == null)
				continue;
			
			// An unaligned node's partner must have the same parent
			if (a.getParent().getPartner() != b.getParent())
				continue;
			
			// An unaligned node must not  be part of the LCS
			if ((i < s.size()) && (a == s.get(i)))
			{
				i += 2;
				continue;
			}
			
			// We've found an aligned node
			editScript.add(new EditOpMove(
					a,
					nLeft,
					b,
					b.indexOf()));
			
			++moves;
		}
	}
	
	// =========================================================================
	
	private static boolean equals(DiffNode a, DiffNode b)
	{
		return a.getPartner() == b;
	}
	
	// =========================================================================
	
	private static final class MutualPartnerComparator
			implements
				ElementComparatorInterface<DiffNode>
	{
		@Override
		public boolean equals(DiffNode a, DiffNode b)
		{
			return ChildrenAligner.equals(a, b);
		}
	}
}
