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

public class WomDiffNodeAdapterText
		extends
			WomDiffNodeAdapterTextContainer
{
	public WomDiffNodeAdapterText(Wom3Node node)
	{
		super(node);
	}
	
	// =========================================================================
	
	@Override
	public boolean isNonRtdTextLeaf()
	{
		return true;
	}
	
	@Override
	public DiffNode createSame(DiffNode forRoot_)
	{
		return new WomDiffNodeAdapterText(createSameWom(forRoot_));
		
	}
	
	@Override
	public DiffNode splitText(int pos)
	{
		String text = node.getTextContent();
		String ta = text.substring(0, pos);
		String tb = text.substring(pos);
		
		node.setTextContent(ta);
		
		Wom3Node nb = (Wom3Node) node.getOwnerDocument().createElementNS(
				Wom3Node.WOM_NS_URI, "text");
		nb.setTextContent(tb);
		
		WomDiffNodeAdapterText nbWd = new WomDiffNodeAdapterText(nb);
		getParent().appendOrInsert(nbWd, getNextSibling());
		return nbWd;
	}
}
