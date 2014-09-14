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
package de.fau.cs.osr.hddiff.perfsuite;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.sweble.wom3.Wom3Attribute;
import org.sweble.wom3.Wom3Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.fau.cs.osr.hddiff.editscript.EditOp;
import de.fau.cs.osr.hddiff.perfsuite.editscript.EditScriptBuilder;
import de.fau.cs.osr.hddiff.perfsuite.util.PageRevDiffNode;
import de.fau.cs.osr.hddiff.perfsuite.util.SerializationUtils;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.utils.ComparisonException;

public class RunFcDiff
{
	private static final String NS_DIFF = "http://www.hiit.fi/fc/xml/tdm/diff";
	
	private static final String NS_REF = "http://www.hiit.fi/fc/xml/ref";
	
	private final SerializationUtils serialization;
	
	private DiffNode root1;
	
	private DiffNode root2;
	
	// =========================================================================
	
	public RunFcDiff()
	{
		this.serialization = new SerializationUtils(RunXyDiff.class.getSimpleName());
	}
	
	// =========================================================================
	
	public EditScriptAnalysis run(
			PageRevDiffNode prdn,
			File fileA,
			File fileB)
	{
		File fileDiff = null;
		try
		{
			fileDiff = serialization.createTempDiffFile(prdn);
			
			runFcDiff(fileA, fileB, fileDiff);
			
			Document doc = serialization.parseXml(fileDiff);
			
			root1 = prdn.nodeA;
			root2 = prdn.nodeB;
			
			map(root1, root2);
			evaluateDiff(doc.getDocumentElement(), prdn.nodeA, prdn.nodeB, 0);
			
			List<EditOp> editScript = EditScriptBuilder.buildEditScript(
					prdn.nodeA,
					prdn.nodeB,
					null);
			
			return new EditScriptAnalysis(editScript);
		}
		catch (Exception e)
		{
			if (e instanceof FcDiffException)
				throw (FcDiffException) e;
			throw new FcDiffException(e);
		}
		finally
		{
			FileUtils.deleteQuietly(fileDiff);
		}
	}
	
	// =========================================================================
	
	private String runFcDiff(File fileA, File fileB, File fileDiff) throws IOException
	{
		PrintStream oldOut = System.out;
		PrintStream oldErr = System.err;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			try
			{
				try (PrintStream ps = new PrintStream(baos))
				{
					System.setOut(ps);
					System.setErr(ps);
					
					fc.xml.diff.Diff.main(new String[] {
							fileA.getAbsolutePath(),
							fileB.getAbsolutePath(),
							fileDiff.getAbsolutePath() });
				}
				finally
				{
					System.setOut(oldOut);
					System.setErr(oldErr);
				}
			}
			catch (Exception e)
			{
				throw new FcDiffException(new String(baos.toByteArray()), e);
			}
			
			return new String(baos.toByteArray());
		}
	}
	
	// =========================================================================
	
	private int evaluateDiff(
			Node diffNode,
			DiffNode parentA,
			DiffNode womB,
			int level) throws ComparisonException
	{
		if (level == 0)
		{
			if (!cmpTag(diffNode, NS_DIFF, "diff"))
				fcDiffConfusesMe();
			
			NodeList children = diffNode.getChildNodes();
			if (children.getLength() != 1)
				fcDiffConfusesMe();
			
			return evaluateDiff(
					(Element) children.item(0),
					parentA,
					womB.getFirstChild(),
					level + 1);
		}
		else
		{
			if (cmpTag(diffNode, NS_REF, "node"))
			{
				/**
				 * The edit script is referencing a node from the original
				 * document. The node's content is refined by the child nodes of
				 * the ref:node element.
				 */
				DiffNode refWomA = getWom(parentA, (Element) diffNode);
				processNodeRef(diffNode, refWomA, refWomA, womB, level);
				return 1;
			}
			else if (cmpTag(diffNode, NS_DIFF, "copy"))
			{
				/**
				 * The edit script is copies a range of sibling elements from
				 * the original document.
				 */
				DiffNode[] refRunA = getWomRun(parentA, (Element) diffNode);
				return copyRun(womB, refRunA);
			}
			else if (diffNode.getNodeType() == Node.TEXT_NODE)
			{
				fcDiffConfusesMe();
				return 1;
			}
			else
			{
				DiffNode newWomA = womB.createSame(root1);
				processNewNode(diffNode, parentA, newWomA, womB, level);
				
				/*
				Wom3Node nativeNode = (Wom3Node) newWomA.getNativeNode();
				if (cmpStr(diffNode.getNamespaceURI(), nativeNode.getNamespaceURI()) &&
						cmpStr(diffNode.getLocalName(), nativeNode.getLocalName()))
					map();
				*/
				
				return 1;
			}
		}
	}
	
	private void processNodeRef(
			Node diffNode,
			DiffNode parentA,
			DiffNode refWomA,
			DiffNode womB,
			int level) throws ComparisonException
	{
		NodeList diffChildren = diffNode.getChildNodes();
		
		if (diffChildren.getLength() == 0)
		{
			/**
			 * I guess an empty ref:node means that referenced node and new node
			 * must be identical (deep).
			 */
			
			compareDeep(getWomNode(refWomA), getWomNode(womB));
			mapDeep(refWomA, womB);
		}
		else if (refWomA.isTextLeaf())
		{
			if (diffChildren.getLength() == 0)
			{
				// fcdiff screws up spaces so this can happen for whitespace only text nodes.
			}
			else
			{
				if (diffChildren.getLength() != 1)
					fcDiffConfusesMe();
				processTextNodeContent(parentA, refWomA, womB, diffChildren);
			}
			map(refWomA, womB);
		}
		else
		{
			compare(getWomNode(refWomA), getWomNode(womB));
			map(refWomA, womB);
			processChildren(parentA, womB, level, diffChildren);
		}
	}
	
	private void processNewNode(
			Node diffNode,
			DiffNode parentA,
			DiffNode newWomA,
			DiffNode womB,
			int level) throws ComparisonException
	{
		NodeList diffChildren = diffNode.getChildNodes();
		
		if (newWomA.isTextLeaf())
		{
			if (diffChildren.getLength() == 0)
			{
				// fcdiff screws up spaces so this can happen for whitespace only text nodes.
			}
			else
			{
				if (diffChildren.getLength() != 1)
					fcDiffConfusesMe();
				processTextNodeContent(parentA, newWomA, womB, diffChildren);
			}
		}
		else
		{
			processChildren(parentA, womB, level, diffChildren);
		}
	}
	
	private void processTextNodeContent(
			DiffNode parentA,
			DiffNode refWomA,
			DiffNode womB,
			NodeList diffChildren)
	{
		@SuppressWarnings("unused")
		String diffText;
		
		Node text = diffChildren.item(0);
		if (text.getNodeType() != Node.TEXT_NODE)
		{
			if (!cmpTag(text, NS_DIFF, "copy"))
				fcDiffConfusesMe();
			
			DiffNode[] run = getWomRun(parentA, (Element) text);
			if ((run.length != 1) || (run[0].getClass() != refWomA.getClass()))
				fcDiffConfusesMe();
			
			diffText = run[0].getTextContent();
		}
		else
		{
			diffText = text.getTextContent();
		}
		
		// no use, fcdiff messes with spaces :(
		/*
		diffText = StringEscapeUtils.unescapeXml(diffText);
		diffText = diffText.replaceAll("\\s+", "");
		String newText = womB.getTextContent();
		newText = newText.replaceAll("\\s+", "");
		
		if (!cmpStr(diffText, newText))
			fcDiffConfusesMe();
		*/
	}
	
	private void processChildren(
			DiffNode parentA,
			DiffNode womB,
			int level,
			NodeList diffChildren) throws ComparisonException
	{
		DiffNode childB = womB.getFirstChild();
		
		for (int i = 0; i < diffChildren.getLength(); ++i, childB = childB.getNextSibling())
		{
			Node childDiff = diffChildren.item(i);
			
			if (childB == null)
				fcDiffConfusesMe();
			
			int run = evaluateDiff(
					childDiff,
					parentA,
					childB,
					level + 1);
			
			for (int k = 1; k < run; ++k)
			{
				childB = childB.getNextSibling();
				if (childB == null)
					fcDiffConfusesMe();
			}
		}
		
		if (childB != null)
			fcDiffConfusesMe();
	}
	
	private int copyRun(DiffNode womB, DiffNode[] refRunA) throws ComparisonException
	{
		DiffNode curB = womB;
		for (int i = 0; i < refRunA.length; ++i)
		{
			DiffNode refWomA = refRunA[i];
			compareDeep(getWomNode(refWomA), getWomNode(curB));
			mapDeep(refWomA, curB);
			curB = curB.getNextSibling();
		}
		
		return refRunA.length;
	}
	
	private DiffNode getWom(DiffNode womA, Element refNode)
	{
		return getWom(womA, refNode, "id");
	}
	
	private DiffNode getWom(DiffNode womA, Element refNode, String attrName)
	{
		String id = refNode.getAttribute(attrName);
		if (id == null)
			fcDiffConfusesMe();
		if (id.startsWith("/"))
		{
			return getWomRec(root1, id.substring(1));
		}
		else
		{
			if (!id.startsWith("./"))
				fcDiffConfusesMe();
			return getWomRec(womA, id.substring(2));
		}
	}
	
	private Wom3Node getWomNode(DiffNode womA)
	{
		return (Wom3Node) womA.getNativeNode();
	}
	
	private DiffNode getWomRec(DiffNode womA, String relPath)
	{
		int i = relPath.indexOf('/');
		
		String childNumStr = relPath;
		if (i != -1)
			childNumStr = relPath.substring(0, i);
		
		int childNum = -1;
		try
		{
			childNum = Integer.parseInt(childNumStr);
		}
		catch (NumberFormatException e)
		{
			fcDiffConfusesMe();
		}
		
		/**
		 * This is damn ugly...
		 */
		if (womA.isTextLeaf())
		{
			if ((i != -1) || (childNum != 0))
				fcDiffConfusesMe();
			return womA;
		}
		
		DiffNode child = womA.getFirstChild();
		for (int j = 0; j != childNum; ++j)
			child = child.getNextSibling();
		if (i == -1)
			return child;
		
		return getWomRec(child, relPath.substring(i + 1));
	}
	
	private DiffNode[] getWomRun(DiffNode womA, Element refNode)
	{
		DiffNode first = getWom(womA, refNode, "src");
		String runStr = refNode.getAttribute("run");
		if (runStr == null)
			fcDiffConfusesMe();
		
		int runCount = -1;
		try
		{
			runCount = Integer.parseInt(runStr);
		}
		catch (NumberFormatException e)
		{
			fcDiffConfusesMe();
		}
		
		DiffNode[] run = new DiffNode[runCount];
		DiffNode cur = first;
		for (int i = 0; i < runCount; ++i)
		{
			run[i] = cur;
			cur = cur.getNextSibling();
		}
		
		return run;
	}
	
	private boolean cmpTag(Node node, String ns, String tag)
	{
		if (node.getNodeType() != Node.ELEMENT_NODE)
			return false;
		String localName = node.getLocalName();
		if (localName == null)
			localName = node.getNodeName();
		return cmpStr(node.getNamespaceURI(), ns) && cmpStr(localName, tag);
	}
	
	private boolean cmpStr(String a, String b)
	{
		return (a == null) ? (b == null) : a.equals(b);
	}
	
	private void map(DiffNode n1, DiffNode n2)
	{
		n1.set(n2, 0);
		n2.set(n1, 0);
	}
	
	private void mapDeep(DiffNode n1, DiffNode n2)
	{
		map(n1, n2);
		for (DiffNode c1 = n1.getFirstChild(), c2 = n2.getFirstChild(); c1 != null; c1 = c1.getNextSibling(), c2 = c2.getNextSibling())
			mapDeep(c1, c2);
	}
	
	private void compareDeep(Wom3Node a, Wom3Node b) throws ComparisonException
	{
		compare(a, b);
		if (!compareChildren(a.getWomChildNodes(), b.getWomChildNodes()))
		{
			throw new ComparisonException(a, b);
		}
	}
	
	private void compare(Wom3Node a, Wom3Node b) throws ComparisonException
	{
		if (a.getNodeType() != b.getNodeType())
		{
			throw new ComparisonException(a, b);
		}
		if (!cmpStr(a.getNodeName(), b.getNodeName()))
		{
			throw new ComparisonException(a, b);
		}
		if (!cmpStr(a.getNamespaceURI(), b.getNamespaceURI()))
		{
			throw new ComparisonException(a, b);
		}
		if (!cmpStr(a.getNodeValue(), b.getNodeValue()))
		{
			//if (!(StringUtils.isWhitespace(a.getNodeValue()) && StringUtils.isWhitespace(b.getNodeValue())))
			//	throw new ComparisonException(a, b);
		}
		if (!compareAttributes(a.getWomAttributes(), b.getWomAttributes()))
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
			if (!cmpStr(attrA.getName(), attrB.getName()))
			{
				return false;
			}
			if (!cmpStr(attrA.getValue(), attrB.getValue()))
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
			Collection<Wom3Node> b) throws ComparisonException
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
	
	// =========================================================================
	
	private void fcDiffConfusesMe()
	{
		throw new FcDiffException("FcDiff output syntax confuses me!");
	}
	
	// =========================================================================
	
	public static final class FcDiffException
			extends
				RuntimeException
	{
		private static final long serialVersionUID = 1L;
		
		public FcDiffException(String message)
		{
			super(message);
		}
		
		public FcDiffException(Throwable cause)
		{
			super(cause);
		}
		
		public FcDiffException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}
}
