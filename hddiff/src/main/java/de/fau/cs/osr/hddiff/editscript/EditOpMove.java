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
 * A node was found in the right tree whose parent does not match the respective
 * parent node of the node's partner in the left tree. The move operation
 * describes how to transform the left tree so that it is equal to the right
 * tree.
 */
public class EditOpMove
		implements
			EditOp
{
	private final DiffNode movedNode;

	private final DiffNode toParent;

	private final int finalPosition;

	/**
	 * Additional information: The node in the right tree after the move.
	 */
	private final DiffNode movedNodeRight;

	// =========================================================================

	public EditOpMove(
			DiffNode movedNode,
			DiffNode toParent,
			DiffNode movedNodeRight,
			int finalPosition)
	{
		this.movedNode = movedNode;
		this.toParent = toParent;
		this.movedNodeRight = movedNodeRight;
		this.finalPosition = finalPosition;
	}

	// =========================================================================

	@Override
	public Operation getType()
	{
		return Operation.MOVE;
	}

	// =========================================================================

	public DiffNode getMovedNode()
	{
		return movedNode;
	}

	public DiffNode getToParent()
	{
		return toParent;
	}

	public int getFinalPosition()
	{
		return finalPosition;
	}

	public DiffNode getMovedNodeRight()
	{
		return movedNodeRight;
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
				StringTools.indent(movedNode.toString(), "    "),
				StringTools.indent(toParent.toString(), "    "));
	}
}
