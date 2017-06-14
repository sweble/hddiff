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
package de.fau.cs.osr.hddiff.tree;

import org.apache.commons.lang3.StringUtils;

import de.fau.cs.osr.utils.ComparisonException;

public abstract class DiffNode
{
	private DiffNode parent;

	private DiffNode prevSibling;

	private DiffNode nextSibling;

	private DiffNode firstChild;

	private DiffNode lastChild;

	// =========================================================================

	private DiffNode partner;

	/** common in 0..weight. */
	private int common;

	private int weight;

	private int subtreeHash;

	private boolean subtreeMatched;

	/** Debug only. */
	private boolean isSplit;

	/** Debug only. */
	private String color;

	// =========================================================================

	public final void set(DiffNode partner, int common)
	{
		if (this.partner != null)
			throw new RuntimeException("Partner already set!");
		setOverride(partner, common);
	}

	public final void setOverride(DiffNode partner, int common)
	{
		if (!isSameNodeType(partner))
			throw new AssertionError("!isSameNodeType(partner): "
					+ String.valueOf(this.getType()) + " vs. " + String.valueOf(partner.getType()));
		this.partner = partner;
		this.common = common;
	}

	public void unmapDeep()
	{
		if (this.partner != null)
			this.partner.resetMatch();
		resetMatch();
		for (DiffNode n = getFirstChild(); n != null; n = n.getNextSibling())
			n.unmapDeep();
	}

	private void resetMatch()
	{
		this.partner = null;
		this.weight = 0;
		this.common = 0;
		this.subtreeMatched = false;
		this.isSplit = false;
	}

	// =========================================================================

	public DiffNode getPartner()
	{
		return partner;
	}

	public int getCommon()
	{
		return common;
	}

	public int getWeight()
	{
		return weight;
	}

	public void setWeight(int weight)
	{
		this.weight = weight;
	}

	public int addWeight(int weight)
	{
		this.weight += weight;
		return this.weight;
	}

	public int getSubtreeHash()
	{
		return subtreeHash;
	}

	public int updateSubtreeHash(int hash)
	{
		subtreeHash = (17 * subtreeHash) ^ hash;
		return subtreeHash;
	}

	public boolean isSubtreeMatched()
	{
		return subtreeMatched;
	}

	public void setSubtreeMatched(boolean subtreeMatched)
	{
		this.subtreeMatched = subtreeMatched;
	}

	public boolean isSplit()
	{
		return isSplit;
	}

	public void setSplit(boolean isSplit)
	{
		this.isSplit = isSplit;
	}

	public String getColor()
	{
		return color;
	}

	public void setColor(String color)
	{
		this.color = color;
	}

	// =========================================================================

	public final DiffNode getParent()
	{
		return parent;
	}

	public final DiffNode getFirstChild()
	{
		return firstChild;
	}

	public final DiffNode getLastChild()
	{
		return lastChild;
	}

	public final DiffNode getPrevSibling()
	{
		return prevSibling;
	}

	public final DiffNode getNextSibling()
	{
		return nextSibling;
	}

	public final int indexOf()
	{
		int index = 0;
		DiffNode tmp = this;
		while (true)
		{
			tmp = tmp.getPrevSibling();
			if (tmp == null)
				break;
			++index;
		}
		return index;
	}

	public void appendOrInsert(
			DiffNode newChild,
			DiffNode refChild)
	{
		appendOrInsertNativeOnly(newChild, refChild);
		if (refChild == null)
			appendChildDiffOnly(newChild);
		else
			insertBeforeDiffOnly(newChild, refChild);
	}

	public void insertAt(int index, DiffNode node)
	{
		appendOrInsert(node, getRefChild(index));
	}

	public void removeFromParent()
	{
		removeFromParentNativeOnly();
		removeFromParentDiffOnly();
	}

	public void appendChildDiffOnly(DiffNode node)
	{
		if ((node.parent != null) || (node.prevSibling != null) || (node.nextSibling != null))
			throw new IllegalArgumentException("Cannot append linked node");

		node.parent = this;
		if (lastChild != null)
		{
			node.prevSibling = lastChild;
			lastChild.nextSibling = node;
		}
		lastChild = node;
		if (firstChild == null)
			firstChild = node;
	}

	public void insertBeforeDiffOnly(DiffNode newChild, DiffNode refChild)
	{
		if (refChild == null)
			throw new IllegalArgumentException();
		if ((newChild.parent != null) || (newChild.prevSibling != null) || (newChild.nextSibling != null))
			throw new IllegalArgumentException("Cannot append linked node");

		// Link node
		newChild.parent = this;
		newChild.prevSibling = refChild.getPrevSibling();
		newChild.nextSibling = refChild;

		// Link siblings
		if (newChild.prevSibling != null)
			newChild.prevSibling.nextSibling = newChild;
		refChild.prevSibling = newChild;

		// Link parent
		if (refChild == firstChild)
			firstChild = newChild;
	}

	private void removeFromParentDiffOnly()
	{
		// Remove from sibling chain
		if (prevSibling != null)
			prevSibling.nextSibling = nextSibling;
		if (nextSibling != null)
			nextSibling.prevSibling = prevSibling;

		// Remove from parent
		if (parent != null)
		{
			if (parent.firstChild == this)
				parent.firstChild = nextSibling;
			if (parent.lastChild == this)
				parent.lastChild = prevSibling;
		}

		// Unlink self
		prevSibling = null;
		nextSibling = null;
		parent = null;
	}

	public DiffNode getRefChild(int newIndex)
	{
		int i = newIndex;
		DiffNode refChild = getFirstChild();
		while ((refChild != null) && (i > 0))
		{
			refChild = refChild.getNextSibling();
			--i;
		}
		return refChild;
	}

	// =========================================================================

	@Override
	public String toString()
	{
		String nodeStr = StringUtils.abbreviateMiddle(getNativeNode().toString(), "...", 32);
		String partnerStr = null;
		if (getPartner() != null)
			partnerStr = StringUtils.abbreviateMiddle(getPartner().getNativeNode().toString(), "...", 32);
		return String.format("" +
				"DiffNode:\n" +
				"  WOM node: \"%s\"\n" +
				"  Partner WOM node: %s\n" +
				"  Common: %d\n" +
				"  Weight: %d\n" +
				"  Subtree hash: %d\n",
				nodeStr,
				(partnerStr == null ? "" : "\"" + partnerStr + "\""),
				common,
				weight,
				subtreeHash);
	}

	// =========================================================================

	public abstract Object getType();

	public abstract boolean isSameNodeType(DiffNode o);

	public abstract String getLabel();

	public abstract Object getNativeNode();

	// =========================================================================

	public abstract DiffNode createSame(DiffNode forRoot);

	protected abstract void appendOrInsertNativeOnly(
			DiffNode newChild,
			DiffNode refChild);

	protected abstract void removeFromParentNativeOnly();

	// =========================================================================

	/*
	public abstract boolean isNodeValueEqual(DiffNode o);

	public abstract Object getNodeValue();

	public abstract void setNodeValue(Object value);
	*/

	/** @return Returns null if no update is necessary. */
	public abstract NodeUpdate compareWith(DiffNode o);
	
	public abstract void applyUpdate(NodeUpdate nodeUpdate);

	// =========================================================================

	public abstract boolean isLeaf();

	public abstract boolean isTextLeaf();

	public abstract String getTextContent();

	public abstract DiffNode splitText(int pos);

	// =========================================================================

	public abstract void compareNativeDeep(DiffNode o) throws ComparisonException;

	public abstract void setNativeId(String id);
}
