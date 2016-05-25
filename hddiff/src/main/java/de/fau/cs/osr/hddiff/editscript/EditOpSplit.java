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
