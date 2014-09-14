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
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.sweble.wom3.Wom3Node;
import org.sweble.wom3.swcadapter.AstToWomConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import de.fau.cs.osr.hddiff.perfsuite.EditScriptAnalysis.Edit;
import de.fau.cs.osr.hddiff.perfsuite.EditScriptAnalysis.GenericEditOp;
import de.fau.cs.osr.hddiff.perfsuite.util.PageRevDiffNode;
import de.fau.cs.osr.hddiff.perfsuite.util.SerializationUtils;
import de.fau.cs.osr.utils.ThreadedStreamReader;
import de.fau.cs.osr.utils.TimeoutProcess;

public class RunXyDiff
{
	public static final String XYDIFF_BIN_PROPERTY_NAME = "xydiff.bin";
	
	// =========================================================================
	
	private final String xyDiffBin;
	
	private final SerializationUtils serialization;
	
	private EditScriptAnalysis editScriptAnalysis;
	
	// =========================================================================
	
	public RunXyDiff()
	{
		this.xyDiffBin = System.getProperty(XYDIFF_BIN_PROPERTY_NAME);
		if ((xyDiffBin == null) || (!new File(xyDiffBin).exists()))
			throw new IllegalArgumentException("JVM system property "
					+ "xydiff.bin not set or not pointing to an existing binary");
		
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
			
			runXyDiff(fileA, fileB, fileDiff);
			
			Document doc = serialization.parseXml(fileDiff);
			
			editScriptAnalysis = new EditScriptAnalysis();
			evaluateDiff(doc.getDocumentElement(), 0, false, GenericEditOp.NONE);
			
			return editScriptAnalysis;
		}
		catch (Exception e)
		{
			if (e instanceof XyDiffException)
				throw (XyDiffException) e;
			throw new XyDiffException(e);
		}
		finally
		{
			removeXidMap(fileA);
			removeXidMap(fileB);
			FileUtils.deleteQuietly(fileDiff);
		}
	}
	
	// =========================================================================
	
	private String runXyDiff(File fileA, File fileB, File fileDiff) throws Exception
	{
		ProcessBuilder pb = new ProcessBuilder(new String[] {
				xyDiffBin,
				"-o", fileDiff.getAbsolutePath(),
				fileA.getAbsolutePath(),
				fileB.getAbsolutePath() });
		
		try (StringWriter w = new StringWriter())
		{
			Process p = pb.start();
			new ThreadedStreamReader(p.getErrorStream(), "ERROR", w);
			new ThreadedStreamReader(p.getInputStream(), "OUTPUT", w);
			
			try
			{
				TimeoutProcess.waitFor(p, 2000);
			}
			catch (Exception e)
			{
				throw new XyDiffException(e);
			}
			
			if (p.exitValue() == 0)
				return w.toString();
			else
				throw new XyDiffException(w.toString());
		}
	}
	
	// =========================================================================
	
	private void evaluateDiff(
			Node node,
			int depth,
			boolean parentIsTextContainer,
			GenericEditOp op)
	{
		String indent = null;
		
		Edit e = null;
		GenericEditOp childOp = op;
		boolean isTextContainer = false;
		boolean descend = true;
		
		String localName = node.getLocalName();
		if (localName == null)
		{
			e = handleXmlText(node, op, parentIsTextContainer, indent);
		}
		else
		{
			String namespaceURI = node.getNamespaceURI();
			
			if (Wom3Node.WOM_NS_URI.equals(namespaceURI) || AstToWomConverter.MWW_NS_URI.equals(namespaceURI))
			{
				if ((op == null) || (op == GenericEditOp.NONE))
					xyDiffConfusesMe();
				
				e = new Edit(op, localName, null);
				
				isTextContainer = ("text".equals(localName) || "rtd".equals(localName));
			}
			else if ("urn:schemas-xydiff:xydelta".equals(namespaceURI))
			{
				if (op != null && op != GenericEditOp.NONE)
					xyDiffConfusesMe();
				
				if ("unit_delta".equals(localName) || "t".equals(localName))
				{
					// Don't know what that means...
				}
				else if ("d".equals(localName))
				{
					Node namedItem = node.getAttributes().getNamedItem("move");
					if (namedItem != null && "yes".equalsIgnoreCase(namedItem.getNodeValue()))
					{
						// We only count moves on insert, not delete
					}
					else
					{
						childOp = GenericEditOp.DEL;
					}
				}
				else if ("i".equals(localName))
				{
					Node namedItem = node.getAttributes().getNamedItem("move");
					if (namedItem != null && "yes".equalsIgnoreCase(namedItem.getNodeValue()))
					{
						childOp = GenericEditOp.MOV;
						e = new Edit(GenericEditOp.MOV, "#move", null);
					}
					else
					{
						childOp = GenericEditOp.INS;
					}
				}
				else if ("au".equals(localName) || "ai".equals(localName) || "ad".equals(localName))
				{
					// We ignore attribute update/insert/delete
				}
				else if ("u".equals(localName))
				{
					e = evaluateUpdate(node);
					descend = false;
				}
				else
				{
					xyDiffConfusesMe();
				}
			}
			else
			{
				xyDiffConfusesMe();
			}
		}
		
		// Insert/move/update top-down
		if ((e != null) && (e.op != GenericEditOp.DEL))
			editScriptAnalysis.add(e);
		
		if (descend)
		{
			NodeList children = node.getChildNodes();
			int len = children.getLength();
			for (int i = 0; i < len; ++i)
			{
				Node child = children.item(i);
				evaluateDiff(child, depth + 1, isTextContainer, childOp);
			}
		}
		
		// Delete bottom-up
		if ((e != null) && (e.op == GenericEditOp.DEL))
			editScriptAnalysis.add(e);
	}
	
	private Edit handleXmlText(
			Node node,
			GenericEditOp op,
			boolean parentIsTextContainer,
			String indent)
	{
		String str = ((Text) node).getNodeValue();
		if (parentIsTextContainer)
		{
			if ((op == null) || (op == GenericEditOp.NONE))
				xyDiffConfusesMe();
			
			return new Edit(op, null, str);
		}
		else
		{
			// Ignore whitespace in between tags
			if (!str.trim().isEmpty())
			{
				if ((op == null) || (op == GenericEditOp.NONE))
					xyDiffConfusesMe();
				
				return new Edit(op, null, str);
			}
			return null;
		}
	}
	
	private Edit evaluateUpdate(Node node)
	{
		int ins = 0;
		int del = 0;
		
		NodeList children = node.getChildNodes();
		int len = children.getLength();
		for (int i = 0; i < len; ++i)
		{
			Node child = children.item(i);
			
			String localName = child.getLocalName();
			if (localName == null)
			{
				String str = ((Text) child).getNodeValue();
				// Ignore whitespace in between tags
				if (!str.trim().isEmpty())
					xyDiffConfusesMe();
			}
			else
			{
				String namespaceURI = child.getNamespaceURI();
				if (!"urn:schemas-xydiff:xydelta".equals(namespaceURI))
					xyDiffConfusesMe();
				
				if ("td".equals(localName))
				{
					Node namedItem = child.getAttributes().getNamedItem("len");
					if (namedItem == null)
						xyDiffConfusesMe();
					del += Integer.valueOf(namedItem.getNodeValue());
				}
				else if ("ti".equals(localName))
				{
					ins += child.getTextContent().length();
				}
				else if ("tr".equals(localName))
				{
					Node namedItem = child.getAttributes().getNamedItem("len");
					if (namedItem == null)
						xyDiffConfusesMe();
					del += Integer.valueOf(namedItem.getNodeValue());
					ins += child.getTextContent().length();
				}
				else
					xyDiffConfusesMe();
			}
		}
		
		return new Edit(GenericEditOp.UPD, ins, del);
	}
	
	// =========================================================================
	
	private void xyDiffConfusesMe()
	{
		throw new RuntimeException("XyDiff output syntax confuses me!");
	}
	
	private void removeXidMap(File fileA)
	{
		if (fileA == null)
			return;
		FileUtils.deleteQuietly(new File(fileA + ".xidmap"));
	}
	
	// =========================================================================
	
	public static final class XyDiffException
			extends
				RuntimeException
	{
		private static final long serialVersionUID = 1L;
		
		public XyDiffException(String message)
		{
			super(message);
		}
		
		public XyDiffException(Throwable cause)
		{
			super(cause);
		}
		
		public XyDiffException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}
}
