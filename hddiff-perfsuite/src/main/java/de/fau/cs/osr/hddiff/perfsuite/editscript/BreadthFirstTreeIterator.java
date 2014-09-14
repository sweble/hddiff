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
import java.util.NoSuchElementException;

import de.fau.cs.osr.hddiff.tree.DiffNode;

public class BreadthFirstTreeIterator
		implements
			TreeIterator
{
	private final LinkedList<DiffNode> queue = new LinkedList<>();
	
	// =========================================================================
	
	public BreadthFirstTreeIterator(DiffNode root)
	{
		if (root == null)
			throw new NullPointerException();
		queue.add(root);
	}
	
	// =========================================================================
	
	@Override
	public boolean hasNext()
	{
		return !queue.isEmpty();
	}
	
	@Override
	public DiffNode next()
	{
		if (!hasNext())
			throw new NoSuchElementException();
		
		DiffNode node = queue.removeFirst();
		DiffNode child = node.getFirstChild();//tree.getFirstChild(node);
		while (child != null)
		{
			queue.addLast(child);
			child = child.getNextSibling();//tree.getChildAfter(node, child);
		}
		
		return node;
	}
	
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}
