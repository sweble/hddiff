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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sweble.wom3.Wom3Document;
import org.sweble.wom3.Wom3Element;
import org.sweble.wom3.Wom3Link;
import org.sweble.wom3.Wom3Node;
import org.sweble.wom3.Wom3Repl;
import org.sweble.wom3.Wom3Rtd;
import org.sweble.wom3.Wom3Text;
import org.sweble.wom3.swcadapter.AstToWomConverter;

import de.fau.cs.osr.hddiff.tree.DiffNode;

public class WomToDiffNodeConverter
{
	/**
	 * \s*
	 * 
	 * ([^\s\[]+)?
	 * 
	 * \[\[
	 * 
	 * \s*
	 * 
	 * (.+?) \s*(?:\]|\||$)
	 */
	protected static final Pattern LINK_OPEN_RX =
			Pattern.compile("\\s*([^\\s\\[]+)?\\[\\[\\s*(.+?)\\s*(?:\\]|\\||$)");
	
	/**
	 * \]\]
	 * 
	 * (\S.*) \s*$
	 */
	protected static final Pattern LINK_CLOSE_RX =
			Pattern.compile("\\]\\](\\S.*?)\\s*$");
	
	// =========================================================================
	
	public static DiffNode preprocess(Wom3Node wom)
	{
		Wom3Document ownerDocument = wom.getOwnerDocument();
		if (ownerDocument == null)
			ownerDocument = (Wom3Document) wom;
		ownerDocument.setStrictErrorChecking(false);
		return new WomToDiffNodeConverter(ownerDocument).dispatch(wom);
	}
	
	// =========================================================================
	
	private final Wom3Document doc;
	
	// =========================================================================
	
	public WomToDiffNodeConverter(Wom3Document wom3Document)
	{
		this.doc = wom3Document;
	}
	
	// =========================================================================
	
	public DiffNode dispatch(Wom3Node node)
	{
		if (node instanceof Wom3Element)
		{
			return dispatch((Wom3Element) node);
		}
		else if (node instanceof Wom3Link)
		{
			return visit((Wom3Link) node);
		}
		else if (node instanceof Wom3Repl)
		{
			return visit((Wom3Repl) node);
		}
		else if (node instanceof Wom3Rtd)
		{
			return visit((Wom3Rtd) node);
		}
		else if (node instanceof Wom3Text)
		{
			return visit((Wom3Text) node);
		}
		else
		{
			return iterate(node);
		}
	}
	
	private DiffNode dispatch(Wom3Element node)
	{
		if (AstToWomConverter.MWW_NS_URI.equals(node.getNamespaceURI()))
		{
			if ("intlink".equals(node.getLocalName()))
			{
				return visitMwwIntLink(node);
			}
		}
		
		return iterate(node);
	}
	
	private DiffNode iterate(Wom3Node node)
	{
		return iterate(new WomDiffNodeAdapter(node), node);
	}
	
	private DiffNode iterate(WomDiffNodeAdapter dn, Wom3Node node)
	{
		for (Wom3Node child : node)
		{
			DiffNode dnChild = dispatch(child);
			if (dnChild != null)
				dn.appendChildDiffOnly(dnChild);
		}
		return dn;
	}
	
	// =========================================================================
	
	private DiffNode visit(Wom3Text node)
	{
		return new WomDiffNodeAdapterText(node);
	}
	
	private DiffNode visit(Wom3Rtd node)
	{
		return new WomDiffNodeAdapterRtd(node);
	}
	
	private DiffNode visit(Wom3Repl node)
	{
		Wom3Node parentNode = node.getParentNode();
		if (parentNode != null)
			parentNode.removeChild(node);
		return null;
	}
	
	private DiffNode visit(Wom3Link node)
	{
		return treatInternalLink(node);
	}
	
	private DiffNode visitMwwIntLink(Wom3Element node)
	{
		return treatInternalLink(node);
	}
	
	// =========================================================================
	
	private DiffNode treatInternalLink(Wom3Node node)
	{
		Wom3Node child = node.getFirstChild();
		while (child != null)
		{
			if (child instanceof Wom3Rtd)
			{
				Wom3Rtd rtd = (Wom3Rtd) child;
				String text = rtd.getTextContent();
				Matcher m = LINK_OPEN_RX.matcher(text);
				if (m.find())
				{
					Wom3Node insertBefore = rtd.getNextSibling();
					
					if (m.group(1) != null)
					{
						rtd.setTextContent(text.substring(0, m.start(1)));
						insertBeforeOrAppend(
								node,
								createMwwTextContainerNode("prefix", m.group(1)),
								insertBefore);
					}
					else
					{
						rtd.setTextContent(text.substring(0, m.start(2)));
					}
					
					String target = m.group(2);
					insertBeforeOrAppend(
							node,
							createMwwTextContainerNode("target", target),
							insertBefore);
					
					if (m.end(2) < text.length())
					{
						String closeStr = text.substring(m.end(2));
						Wom3Rtd closeRtd = createWomRtd(closeStr);
						insertBeforeOrAppend(node, closeRtd, insertBefore);
						
						separatePostfix(node, closeRtd, closeStr, insertBefore);
					}
				}
				else
				{
					Wom3Node insertBefore = rtd.getNextSibling();
					separatePostfix(node, rtd, text, insertBefore);
				}
			}
			/*
			else
			{
				dispatch(child);
			}
			*/
			
			child = child.getNextSibling();
		}
		
		return iterate(node);
	}
	
	private void separatePostfix(
			Wom3Node node,
			Wom3Rtd rtd,
			String text,
			Wom3Node insertBefore)
	{
		Matcher m2 = LINK_CLOSE_RX.matcher(text);
		if (m2.find())
		{
			rtd.setTextContent(text.substring(0, m2.start(1)));
			insertBeforeOrAppend(
					node,
					createMwwTextContainerNode("postfix", m2.group(1)),
					insertBefore);
			
			if (m2.end(1) < text.length())
			{
				String after = text.substring(m2.end(1));
				insertBeforeOrAppend(node, createWomRtd(after), insertBefore);
			}
		}
	}
	
	// =========================================================================
	
	private Wom3Element createMwwTextContainerNode(String tag, String text)
	{
		Wom3Element targetElem = (Wom3Element) doc.createElementNS(
				AstToWomConverter.MWW_NS_URI,
				AstToWomConverter.DEFAULT_MWW_NS_PREFIX + ":" + tag);
		targetElem.appendChild(createWomText(text));
		return targetElem;
	}
	
	private Wom3Text createWomText(String text)
	{
		Wom3Text textElem = (Wom3Text) doc.createElementNS(
				Wom3Node.WOM_NS_URI,
				"text");
		textElem.setTextContent(text);
		return textElem;
	}
	
	private Wom3Rtd createWomRtd(String text)
	{
		Wom3Rtd textElem = (Wom3Rtd) doc.createElementNS(
				Wom3Node.WOM_NS_URI,
				"rtd");
		textElem.setTextContent(text);
		return textElem;
	}
	
	private void insertBeforeOrAppend(
			Wom3Node parent,
			Wom3Node apendee,
			Wom3Node before)
	{
		if (before != null)
			parent.insertBefore(apendee, before);
		else
			parent.appendChild(apendee);
	}
}
