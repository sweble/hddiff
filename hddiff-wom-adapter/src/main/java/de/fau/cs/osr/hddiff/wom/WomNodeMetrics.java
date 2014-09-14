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
package de.fau.cs.osr.hddiff.wom;

import org.sweble.wom3.Wom3Node;

import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.tree.NodeMetricsInterface;

public final class WomNodeMetrics
		implements
			NodeMetricsInterface
{
	@Override
	public int computeHash(DiffNode node)
	{
		Wom3Node n = ((WomDiffNodeAdapter) node).getWomNode();
		int hash = (21613 * n.getNodeType()) ^
				n.getNodeName().hashCode() ^
				getNamespaceUriHashCode(n);
		if (node.isTextLeaf())
			hash ^= node.getTextContent().hashCode();
		return hash;
	}
	
	private int getNamespaceUriHashCode(Wom3Node node)
	{
		String uri = node.getNamespaceURI();
		if (uri == null)
			return 62401;
		return uri.hashCode();
	}
	
	@Override
	public int computeWeight(DiffNode node_)
	{
		WomDiffNodeAdapter node = (WomDiffNodeAdapter) node_;
		if (node.isTextLeaf())
		{
			if (node.isRtd())
			{
				return 0;
			}
			else
			{
				return (int) node.getTextContent().length();
			}
		}
		else
		{
			return 3;
		}
	}
	
	@Override
	public boolean verifyHashEquality(DiffNode n1__, DiffNode n2__)
	{
		WomDiffNodeAdapter n1_ = (WomDiffNodeAdapter) n1__;
		WomDiffNodeAdapter n2_ = (WomDiffNodeAdapter) n2__;
		Wom3Node n1 = n1_.getWomNode();
		Wom3Node n2 = n2_.getWomNode();
		
		if (n1.getNodeType() != n2.getNodeType())
			return false;
		if (!n1.getNodeName().equals(n2.getNodeName()))
			return false;
		if (!compareStrings(n1.getNamespaceURI(), n2.getNamespaceURI()))
			return false;
		if (n1_.isTextLeaf() && !compareStrings(n1.getTextContent(), n2.getTextContent()))
			return false;
		return true;
	}
	
	private boolean compareStrings(String a, String b)
	{
		return (a == null) ? (b == null) : a.equals(b);
	}
}
