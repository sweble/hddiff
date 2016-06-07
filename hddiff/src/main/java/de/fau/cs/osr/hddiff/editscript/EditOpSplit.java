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

public class EditOpSplit
		implements
			EditOp
{
	private final DiffNode splitNode;

	private final int splitPos;

	private final DiffNode otherHalf;

	// =========================================================================

	public EditOpSplit(DiffNode node, int pos, DiffNode otherHalf)
	{
		this.splitNode = node;
		this.splitPos = pos;
		this.otherHalf = otherHalf;
	}

	// =========================================================================

	@Override
	public Operation getType()
	{
		return Operation.SPLIT;
	}

	// =========================================================================

	public DiffNode getSplitNode()
	{
		return splitNode;
	}

	public int getSplitPos()
	{
		return splitPos;
	}

	public DiffNode getOtherHalf()
	{
		return otherHalf;
	}

	// =========================================================================

	@Override
	public String toString()
	{
		return String.format("" +
				"DiffNodeEditOpSplit:\n" +
				"  splitNode:\n%s\n" +
				"  splitPos = %d\n" +
				"  otherHalf:\n%s\n",
				StringTools.indent(splitNode.toString(), "    "),
				splitPos,
				StringTools.indent(otherHalf.toString(), "    "));
	}
}
