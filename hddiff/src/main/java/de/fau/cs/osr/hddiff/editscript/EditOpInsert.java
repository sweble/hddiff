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
package de.fau.cs.osr.hddiff.editscript;

import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.utils.StringTools;

/**
 * A node was found in the right tree that was not present in the left tree. The
 * insert operation describes how to transform the left tree so that it is equal
 * to the right tree.
 */
public class EditOpInsert
		implements
			EditOp
{
	/**
	 * Parent of inserted node in left tree.
	 */
	private final DiffNode parent;

	/**
	 * Node inserted in left tree.
	 */
	private final DiffNode insertedNode;

	/**
	 * Final position among its siblings.
	 */
	private final int finalPosition;

	/**
	 * Additional information: The new node as found in the right tree.
	 */
	private final DiffNode insertedNodeRight;

	// =========================================================================

	public EditOpInsert(
			DiffNode parent,
			DiffNode insertedNode,
			DiffNode insertedNodeRight,
			int finalPosition)
	{
		this.parent = parent;
		this.insertedNode = insertedNode;
		this.insertedNodeRight = insertedNodeRight;
		this.finalPosition = finalPosition;
	}

	// =========================================================================

	@Override
	public Operation getType()
	{
		return Operation.INSERT;
	}

	// =========================================================================

	public DiffNode getParent()
	{
		return parent;
	}

	public DiffNode getInsertedNode()
	{
		return insertedNode;
	}

	public int getFinalPosition()
	{
		return finalPosition;
	}

	public DiffNode getInsertedNodeRight()
	{
		return insertedNodeRight;
	}

	// =========================================================================

	@Override
	public String toString()
	{
		return String.format("" +
				"DiffNodeEditOpInsert:\n" +
				"  finalPosition = %d\n" +
				"  insertedNode:\n%s\n" +
				"  parent:\n%s\n",
				finalPosition,
				StringTools.indent(insertedNode.toString(), "    "),
				StringTools.indent(parent.toString(), "    "));
	}
}
