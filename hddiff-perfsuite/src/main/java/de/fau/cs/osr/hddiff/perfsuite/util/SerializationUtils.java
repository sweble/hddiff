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
package de.fau.cs.osr.hddiff.perfsuite.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.sweble.engine.serialization.WomSerializer;
import org.sweble.wom3.Wom3Node;
import org.w3c.dom.Document;

import de.fau.cs.osr.hddiff.perfsuite.model.Revision;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.wom.WomToDiffNodeConverter;

public class SerializationUtils
{
	private final String prefix;
	
	private final File tempDir;
	
	private final WomSerializer serializer;
	
	// =========================================================================
	
	public SerializationUtils(String prefix)
	{
		this.prefix = prefix;
		this.tempDir = new File(System.getProperty("java.io.tmpdir"));
		this.serializer = new WomSerializer();
	}
	
	// =========================================================================
	
	public PageRevWom xmlToWom(PageRevText prtParsed) throws Exception
	{
		Wom3Node womA = serializer.deserialize(
				prtParsed.textA.getBytes("UTF-8"), WomSerializer.SerializationFormat.XML, true);
		
		Wom3Node womB = serializer.deserialize(
				prtParsed.textB.getBytes("UTF-8"), WomSerializer.SerializationFormat.XML, true);
		
		return new PageRevWom(prtParsed.page, prtParsed.revA, prtParsed.revB, womA, womB);
	}
	
	public PageRevDiffNode xmlToDiffNode(PageRevText prtParsed) throws Exception
	{
		PageRevWom prw = xmlToWom(prtParsed);
		DiffNode nodeA = WomToDiffNodeConverter.preprocess(prw.womA);
		DiffNode nodeB = WomToDiffNodeConverter.preprocess(prw.womB);
		return new PageRevDiffNode(prtParsed.page, prtParsed.revA, prtParsed.revB, nodeA, nodeB);
	}
	
	// =========================================================================
	
	public File storeWomNiceTemp(PageRevWom prw, Revision rev, Wom3Node wom) throws Exception
	{
		File file = File.createTempFile(makeNicePrefix(prw, rev), ".xml", tempDir);
		file.deleteOnExit();
		try (FileOutputStream fos = new FileOutputStream(file))
		{
			byte[] str = serializer.serialize(wom, WomSerializer.SerializationFormat.XML, false, true);
			IOUtils.write(str, fos);
		}
		return file;
	}
	
	public File storeWomCompactTemp(
			PageRevDiffNode prdn,
			Revision rev,
			DiffNode node) throws Exception
	{
		File file = File.createTempFile(makeCompactPrefix(prdn, rev), ".xml", tempDir);
		file.deleteOnExit();
		try (FileOutputStream output = new FileOutputStream(file))
		{
			byte[] bytes = serializer.serialize(
					(Wom3Node) node.getNativeNode(),
					WomSerializer.SerializationFormat.XML,
					true,
					false);
			IOUtils.write(bytes, output);
			return file;
		}
	}
	
	private String makeNicePrefix(PageRevWom prw, Revision rev)
	{
		return String.format("%s-p%d-r%d-nice-",
				prefix,
				prw.page.getId(),
				rev.getId());
	}
	
	private String makeCompactPrefix(PageRevDiffNode prdn, Revision rev)
	{
		return String.format("%s-p%d-r%d-compact-",
				prefix,
				prdn.page.getId(),
				rev.getId());
	}
	
	// =========================================================================
	
	public File createTempDiffFile(PageRevDiffNode prdn) throws IOException
	{
		File fileDiff = File.createTempFile(makeDiffPrefix(prdn), ".xml", tempDir);
		fileDiff.deleteOnExit();
		return fileDiff;
	}
	
	public Document parseXml(File fileDiff) throws Exception
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fileDiff);
		return doc;
	}
	
	private String makeDiffPrefix(PageRevDiffNode prdn)
	{
		return String.format("%s-p%d-r%d-r%d-diff-",
				prefix,
				prdn.page.getId(),
				prdn.revA.getId(),
				prdn.revB.getId());
	}
}
