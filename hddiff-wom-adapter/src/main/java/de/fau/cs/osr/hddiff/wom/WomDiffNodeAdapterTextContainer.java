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

import org.sweble.wom3.Wom3Document;
import org.sweble.wom3.Wom3ElementNode;
import org.sweble.wom3.Wom3Node;

import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.tree.NodeUpdate;

public class WomDiffNodeAdapterTextContainer
		extends
			WomDiffNodeAdapter
{
	public WomDiffNodeAdapterTextContainer(Wom3Node node)
	{
		super(node);
	}

	// =========================================================================

	@Override
	public void applyUpdate(NodeUpdate value_)
	{
		Wom3NodeUpdate2 value = (Wom3NodeUpdate2) value_;

		if (value.attributes != null)
			throw new IllegalArgumentException();

		String newValue = value.value;
		if (!compareStrings(node.getTextContent(), newValue))
			node.setTextContent(newValue);
	}

	@Override
	public NodeUpdate compareWith(DiffNode o)
	{
		if (!isSameNodeType(o))
			throw new IllegalArgumentException();

		Wom3Node a = this.node;
		Wom3Node b = ((WomDiffNodeAdapter) o).node;

		if (compareStrings(a.getTextContent(), b.getTextContent()))
			return null;

		return new Wom3NodeUpdate2(null, o.getTextContent());
	}

	// =========================================================================

	protected Wom3ElementNode createSameWom(DiffNode forRoot_)
	{
		WomDiffNodeAdapter forRoot = (WomDiffNodeAdapter) forRoot_;
		Wom3Document doc = forRoot.node.getOwnerDocument();
		if (doc == null)
			doc = (Wom3Document) forRoot.node;

		Wom3Node prototype = node;

		Wom3ElementNode elem = (prototype.getNamespaceURI() == null) ?
				(Wom3ElementNode) doc.createElement(prototype.getNodeName()) :
				(Wom3ElementNode) doc.createElementNS(
						prototype.getNamespaceURI(),
						prototype.getNodeName());

		elem.appendChild(doc.createTextNode(node.getTextContent()));

		return elem;
	}

	// =========================================================================

	@Override
	public boolean isLeaf()
	{
		return true;
	}

	@Override
	public boolean isTextLeaf()
	{
		return true;
	}

	@Override
	public String getTextContent()
	{
		return node.getTextContent();
	}
}
