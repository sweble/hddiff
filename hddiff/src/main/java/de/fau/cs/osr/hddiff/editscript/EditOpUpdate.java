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

/**
 * A node was found in the right tree whose value does not match its partner
 * node in the left tree. The update operation describes how to transform the
 * left tree so that it is equal to the right tree.
 */
public class EditOpUpdate
		implements
			EditOp
{
	private final DiffNode updatedNode;
	
	private final Object newNodeValue;
	
	/**
	 * Additional information: The right, already updated node.
	 */
	private final DiffNode updatedNodeRight;
	
	// =========================================================================
	
	public EditOpUpdate(
			DiffNode updatedNode,
			Object newNodeValue,
			DiffNode updatedNodeRight)
	{
		this.updatedNode = updatedNode;
		this.newNodeValue = newNodeValue;
		this.updatedNodeRight = updatedNodeRight;
	}
	
	// =========================================================================
	
	@Override
	public Operation getType()
	{
		return Operation.UPDATE;
	}
	
	// =========================================================================
	
	public DiffNode getUpdatedNode()
	{
		return updatedNode;
	}
	
	public Object getNewNodeValue()
	{
		return newNodeValue;
	}
	
	public DiffNode getUpdatedNodeRight()
	{
		return updatedNodeRight;
	}
	
	// =========================================================================
	
	@Override
	public String toString()
	{
		return "DiffNodeEditOpUpdate [node=" + updatedNode + ", newNodeValue=" + newNodeValue + "]";
	}
}
