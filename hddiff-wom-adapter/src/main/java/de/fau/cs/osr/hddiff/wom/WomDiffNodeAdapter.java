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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import javax.xml.XMLConstants;

import org.sweble.wom3.Wom3Attribute;
import org.sweble.wom3.Wom3Document;
import org.sweble.wom3.Wom3ElementNode;
import org.sweble.wom3.Wom3Node;

import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.tree.NodeUpdate;
import de.fau.cs.osr.utils.ComparisonException;

public class WomDiffNodeAdapter
		extends
			DiffNode
{
	protected Wom3Node node;

	// =========================================================================

	public WomDiffNodeAdapter(Wom3Node node)
	{
		this.node = node;
	}

	// =========================================================================

	public Wom3Node getWomNode()
	{
		return node;
	}

	@Override
	public Object getNativeNode()
	{
		return getWomNode();
	}

	public boolean isRtd()
	{
		return false;
	}

	public boolean isNonRtdTextLeaf()
	{
		return false;
	}

	// =========================================================================

	@Override
	public Object getType()
	{
		String name = node.getLocalName();
		if (name == null)
			return node.getNodeName();
		return node.getNamespaceURI() + node.getLocalName();
	}

	@Override
	public boolean isSameNodeType(DiffNode o)
	{
		return getType().equals(o.getType());
	}

	@Override
	public String getLabel()
	{
		return node.getNodeName();
	}

	// =========================================================================

	@Override
	public DiffNode createSame(DiffNode forRoot_)
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

		copyAttributes(elem, prototype.getWomAttributes());

		return new WomDiffNodeAdapter(elem);
	}

	@Override
	protected void appendOrInsertNativeOnly(
			DiffNode newChild_,
			DiffNode refChild_)
	{
		WomDiffNodeAdapter newChild = (WomDiffNodeAdapter) newChild_;
		WomDiffNodeAdapter refChild = (WomDiffNodeAdapter) refChild_;

		if (refChild != null)
			node.insertBefore(newChild.node, refChild.node);
		else
			node.appendChild(newChild.node);
	}

	@Override
	protected void removeFromParentNativeOnly()
	{
		if (node.getParentNode() == null)
			throw new UnsupportedOperationException();
		node.getParentNode().removeChild(node);
	}

	// =========================================================================

	@Override
	public NodeUpdate compareWith(DiffNode o)
	{
		if (!isSameNodeType(o))
			throw new IllegalArgumentException();

		Wom3Node a = this.node;
		Wom3Node b = ((WomDiffNodeAdapter) o).node;

		Collection<Wom3Attribute> aac = a.getWomAttributes();
		Collection<Wom3Attribute> bac = b.getWomAttributes();

		if (!(aac.isEmpty() && bac.isEmpty()))
		{
			if (aac.size() != bac.size())
				return new Wom3NodeUpdate(bac, null);

			Iterator<Wom3Attribute> aai = aac.iterator();
			Iterator<Wom3Attribute> bai = bac.iterator();
			while (aai.hasNext())
			{
				if (!attrEquals(aai.next(), bai.next()))
					return new Wom3NodeUpdate(bac, null);
			}
		}
		return null;
	}

	private boolean attrEquals(Wom3Attribute a, Wom3Attribute b)
	{
		return compareStrings(a.getNamespaceURI(), b.getNamespaceURI()) &&
				compareStrings(a.getPrefix(), b.getPrefix()) &&
				a.getNodeName().equals(b.getNodeName()) &&
				a.getNodeValue().equals(b.getNodeValue());
	}

	@Override
	public void applyUpdate(NodeUpdate value_)
	{
		Wom3NodeUpdate value = (Wom3NodeUpdate) value_;

		if (value.value != null)
			throw new IllegalArgumentException();

		Collection<Wom3Attribute> newAttrs = value.attributes;
		if (node.hasAttributes())
		{
			Wom3ElementNode elem = (Wom3ElementNode) node;
			for (Wom3Attribute a : node.getWomAttributes())
				elem.removeAttributeNode(a);
		}
		if (!newAttrs.isEmpty())
			copyAttributes((Wom3ElementNode) node, newAttrs);
	}

	private void copyAttributes(
			Wom3ElementNode elem,
			Collection<Wom3Attribute> newAttrs)
	{
		for (Wom3Attribute a : newAttrs)
		{
			if (a.getNamespaceURI() != null || a.getPrefix() != null)
				elem.setAttributeNS(a.getNamespaceURI(), a.getNodeName(), a.getNodeValue());
			else
				elem.setAttribute(a.getNodeName(), a.getNodeValue());
		}
	}

	// =========================================================================

	@Override
	public boolean isLeaf()
	{
		return getFirstChild() == null;
	}

	@Override
	public boolean isTextLeaf()
	{
		return false;
	}

	@Override
	public String getTextContent()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public DiffNode splitText(int pos)
	{
		throw new UnsupportedOperationException();
	}

	// =========================================================================

	@Override
	public void compareNativeDeep(DiffNode o) throws ComparisonException
	{
		compare(this.node, ((WomDiffNodeAdapter) o).node);
	}

	private void compare(Wom3Node a, Wom3Node b) throws ComparisonException
	{
		if (a.getNodeType() != b.getNodeType())
		{
			throw new ComparisonException(a, b);
		}
		if (!compareStrings(a.getNodeName(), b.getNodeName()))
		{
			throw new ComparisonException(a, b);
		}
		if (!compareStrings(a.getNamespaceURI(), b.getNamespaceURI()))
		{
			throw new ComparisonException(a, b);
		}
		if (!compareStrings(a.getNodeValue(), b.getNodeValue()))
		{
			throw new ComparisonException(a, b);
		}
		if (!compareAttributes(a.getWomAttributes(), b.getWomAttributes()))
		{
			throw new ComparisonException(a, b);
		}
		if (!compareChildren(a.getWomChildNodes(), b.getWomChildNodes()))
		{
			throw new ComparisonException(a, b);
		}
	}

	private boolean compareAttributes(
			Collection<Wom3Attribute> a,
			Collection<Wom3Attribute> b)
	{
		final int size = a.size();
		Wom3Attribute[] aa = a.toArray(new Wom3Attribute[size]);
		Wom3Attribute[] ba = b.toArray(new Wom3Attribute[b.size()]);
		Comparator<Wom3Attribute> cmp = new Comparator<Wom3Attribute>()
		{
			@Override
			public int compare(Wom3Attribute o1, Wom3Attribute o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		};
		Arrays.sort(aa, cmp);
		Arrays.sort(ba, cmp);
		for (int i = 0, j = 0; i < aa.length && j < ba.length;)
		{
			Wom3Attribute attrA = aa[i];
			Wom3Attribute attrB = ba[i];
			if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attrA.getNamespaceURI())
					&& XMLConstants.XMLNS_ATTRIBUTE.equals(attrA.getName()))
			{
				++i;
				continue;
			}
			if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attrB.getNamespaceURI())
					&& XMLConstants.XMLNS_ATTRIBUTE.equals(attrB.getName()))
			{
				++j;
				continue;
			}
			if (!compareStrings(attrA.getName(), attrB.getName()))
			{
				return false;
			}
			if (!compareStrings(attrA.getValue(), attrB.getValue()))
			{
				return false;
			}
			++i;
			++j;
		}
		return true;
	}

	private boolean compareChildren(
			Collection<Wom3Node> a,
			Collection<Wom3Node> b)
		throws ComparisonException
	{
		if (a.size() != b.size())
		{
			return false;
		}
		Iterator<Wom3Node> ai = a.iterator();
		Iterator<Wom3Node> bi = b.iterator();
		while (ai.hasNext())
		{
			compare(ai.next(), bi.next());
		}
		return true;
	}

	@Override
	public void setNativeId(String id)
	{
		((Wom3ElementNode) node).setAttribute("id", id);
	}

	// =========================================================================

	protected boolean compareStrings(String a, String b)
	{
		return (a != null) ? a.equals(b) : (b == null);
	}

	// =========================================================================

	public static final class Wom3NodeUpdate implements NodeUpdate
	{
		public final Collection<Wom3Attribute> attributes;

		public final String value;

		public Wom3NodeUpdate(Collection<Wom3Attribute> attributes, String value)
		{
			this.attributes = attributes;
			this.value = value;
		}

		@Override
		public String toString()
		{
			if (value != null)
				return "Wom3NodeUpdate [value=" + value + "]";
			else
				return "Wom3NodeUpdate [attributes=" + Arrays.toString(attributes.toArray()) + "]";
		}

		@Override
		public void applyUpdates(Object node)
		{
			throw new UnsupportedOperationException();
		}
	}
}
