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
package de.fau.cs.osr.hddiff.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import de.fau.cs.osr.hddiff.HDDiffOptions;
import de.fau.cs.osr.hddiff.tree.DiffNode;

public class HDDiffTreeVisualizer
{
	private static final String[] colors = {
			"coral", "forestgreen",
			"goldenrod", "lightpink", "lawngreen",
			"saddlebrown", "royalblue", "violet", "crimson",
			"yellow", "darkslateblue" };
	
	private final File bin;
	
	private int nextColor = 0;
	
	// =========================================================================
	
	public static void drawGraph(
			File output,
			DiffNode root1,
			DiffNode root2)
	{
		new HDDiffTreeVisualizer().drawDotGraph(output, root1, root2);
	}
	
	public static void drawGraph(
			File bin,
			File output,
			DiffNode root1,
			DiffNode root2)
	{
		new HDDiffTreeVisualizer(bin).drawDotGraph(output, root1, root2);
	}
	
	public static void drawGraph(
			HDDiffOptions options,
			DiffNode root1,
			DiffNode root2)
	{
		drawGraph(
				options.getGraphvizDotBin(),
				new File(options.getDumpTreesFileTitle()),
				root1,
				root2);
	}
	
	// =========================================================================
	
	public HDDiffTreeVisualizer()
	{
		this(new File("/usr/bin/dot"));
	}
	
	public HDDiffTreeVisualizer(File bin)
	{
		this.bin = bin;
	}
	
	// =========================================================================
	
	private void drawDotGraph(File output, DiffNode root1, DiffNode root2)
	{
		nextColor = 0;
		File dotFile;
		try
		{
			dotFile = new File(URLEncoder.encode(output.getPath(), "UTF-8") + ".dot");
		}
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
			return;
		}
		try (PrintStream ps = new PrintStream(dotFile))
		{
			ps.println("digraph G {");
			
			drawNode(ps, "t1", root1, 0);
			drawNode(ps, "t2", root2, 0);
			
			ps.println("}");
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		
		callDot(dotFile);
	}
	
	private int drawNode(
			PrintStream ps,
			String prefix,
			DiffNode node,
			int i)
	{
		int weight = node.getWeight();
		
		if (node.isTextLeaf())
		{
			String color = getColor(node, null);
			
			String text = node.getTextContent();
			
			String label = abbreviateNodeRep(text);
			
			String shape = "box";
			if (node.isSplit())
				shape = "parallelogram";
			else if (node.isSubtreeMatched())
				shape = "box, peripheries=2";
			else if (weight <= 0)
				shape = "diamond";
			
			String stats = "";
			if (node.getCommon() != Integer.MIN_VALUE)
				stats = String.format("%d/%d\\n", node.getCommon(), weight);
			
			ps.println(String.format(
					"%s_%d [label=\"%s\\\"%s\\\"\", shape=%s, fontsize=8%s];",
					prefix, i, stats, label, shape, color));
		}
		else
		{
			String name = node.getLabel();
			String label = name;
			
			if (node.getCommon() != Integer.MIN_VALUE)
			{
				label = String.format(
						"%d/%d=%d\\n%s",
						node.getCommon(),
						weight,
						(int) (node.getCommon() / (float) weight * 100),
						name);
			}
			
			String color = getColor(node, "gray");
			
			String shape = "plaintext";
			if (node.isSubtreeMatched())
				shape = "box, peripheries=2";
			
			ps.println(String.format(
					"%s_%d [label=\"%s\", shape=%s, fontsize=10%s];",
					prefix, i, label, shape, color));
		}
		
		int j = i + 1;
		for (DiffNode child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			ps.println(String.format(
					"%s_%d -> %s_%d",
					prefix, i,
					prefix, j));
			
			j = drawNode(ps, prefix, child, j);
		}
		
		return j;
	}
	
	private String abbreviateNodeRep(String label)
	{
		label = label.replace("\n", "\\\\n");
		label = label.replace("\"", "\\\"");
		label = StringUtils.abbreviateMiddle(label, "...\n...", 24);
		return label;
	}
	
	private String getColor(DiffNode node, String override)
	{
		String c = null;
		if ((node.getPartner() != null) &&
				// Newly inserted nodes are matched but don't have a parent...
				!((node.getParent() != null) && (node.getPartner().getParent() == null)))
		{
			if (node.getColor() != null)
			{
				c = node.getColor();
			}
			else
			{
				c = override;
				if (c == null)
				{
					c = colors[nextColor];
					nextColor = (nextColor + 1) % colors.length;
				}
				node.setColor(c);
				node.getPartner().setColor(c);
			}
		}
		
		String result = "";
		if (c != null)
		{
			result = String.format(", style=filled, color=\"%s\"", c);
		}
		
		return result;
	}
	
	private void callDot(File inFile)
	{
		try
		{
			String outFname = inFile.getName();
			if (outFname.toLowerCase().endsWith(".dot"))
				outFname = outFname.substring(0, outFname.length() - 4);
			outFname += ".png";
			File outFile = new File(inFile.getParentFile(), outFname);
			
			String[] cmd = new String[] {
					bin.getPath(),
					"-Tpng",
					"-o",
					outFile.getPath(),
					inFile.getPath() };
			
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(cmd);
			
			int size = 0;
			int maxSize = 2 << 16;
			StringWriter sw = new StringWriter();
			BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			
			String line = null;
			while ((line = input.readLine()) != null)
			{
				size += line.length() + 1;
				if (size < maxSize)
				{
					sw.append(line);
					sw.append("\n");
				}
			}
			
			int exitVal = pr.waitFor();
			if (exitVal != 0)
			{
				System.err.println("Command failed:");
				System.err.println(Arrays.toString(cmd));
				System.err.println("Command output:");
				String cmdOutput = sw.toString();
				System.err.println(cmdOutput);
				if (size != cmdOutput.length())
					System.err.println("WARNING: Command output exceeded buffer size!");
			}
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
			e.printStackTrace();
		}
	}
}
