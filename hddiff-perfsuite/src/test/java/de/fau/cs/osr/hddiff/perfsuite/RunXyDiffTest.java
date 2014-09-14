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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.sweble.wikitext.engine.ExpansionCallback;
import org.sweble.wikitext.engine.utils.WtEngineToolbox.TestExpansionCallback;
import org.sweble.wom3.Wom3Document;

import de.fau.cs.osr.hddiff.perfsuite.util.PageRevDiffNode;
import de.fau.cs.osr.hddiff.perfsuite.util.SerializationUtils;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.utils.ReportItem;
import de.fau.cs.osr.hddiff.wom.WomToDiffNodeConverter;
import de.fau.cs.osr.utils.NamedParametrized;
import de.fau.cs.osr.utils.SimpleConsoleOutput;
import de.fau.cs.osr.utils.TestResourcesFixture;

/**
 * Tests if conversion from Wikitext to WOM (using an AST as intermediate
 * representation) works for all basic elements of Wikitext. The parsed Wikitext
 * is expanded. Some elements are also tested with an unexpanded AST.
 */
@RunWith(value = NamedParametrized.class)
public class RunXyDiffTest
{
	private static final String FILTER_RX = ".*?\\.a\\.wikitext";
	
	private static final String INPUT_SUB_DIR = "nopkg-wikidiff";
	
	// =========================================================================
	
	@Parameters
	public static List<Object[]> enumerateInputs() throws Exception
	{
		return HDDiffTestUtils.getTestResourcesFixture()
				.gatherAsParameters(INPUT_SUB_DIR, FILTER_RX, false);
	}
	
	// =========================================================================
	
	private final HDDiffTestUtils utils = new HDDiffTestUtils();
	
	private final File inputFile;
	
	private final SerializationUtils serialization;
	
	// =========================================================================
	
	public RunXyDiffTest(
			String title,
			TestResourcesFixture resources,
			File inputFile)
	{
		this.inputFile = inputFile;
		this.serialization = new SerializationUtils(RunXyDiffTest.class.getSimpleName());
	}
	
	// =========================================================================
	
	@Before
	public void beforeMethod()
	{
		Assume.assumeTrue(System.getProperty(RunXyDiff.XYDIFF_BIN_PROPERTY_NAME) != null);
	}
	
	@Test
	public void testConvertAstToWomAndCompareWithReferenceWom() throws Exception
	{
		SimpleConsoleOutput.printBigSep(inputFile.toString());
		generateAndApplyDiff(inputFile, INPUT_SUB_DIR);
	}
	
	public void generateAndApplyDiff(
			File inputFileA,
			String inputSubDir) throws Exception
	{
		ExpansionCallback callback = new TestExpansionCallback();
		
		Wom3Document womA = utils.parse(inputFileA, callback);
		
		File inputFileB = HDDiffTestUtils.aToB(inputFileA);
		Wom3Document womB = utils.parse(inputFileB, callback);
		
		runXyDiff(inputFileA, womA, womB);
	}
	
	private void runXyDiff(File inputFileA, Wom3Document womA, Wom3Document womB) throws Exception
	{
		DiffNode root1 = WomToDiffNodeConverter.preprocess(womA);
		DiffNode root2 = WomToDiffNodeConverter.preprocess(womB);
		
		PageRevDiffNode prdn = HDDiffTestUtils.makePageRevDiffNode(inputFileA, root1, root2);
		
		File compactWomA = null;
		File compactWomB = null;
		try
		{
			compactWomA = serialization.storeWomCompactTemp(prdn, prdn.revA, prdn.nodeA);
			compactWomB = serialization.storeWomCompactTemp(prdn, prdn.revB, prdn.nodeB);
			
			RunXyDiff xyd = new RunXyDiff();
			
			EditScriptAnalysis esa = xyd.run(prdn, compactWomA, compactWomB);
			
			ReportItem reportItem = new ReportItem();
			esa.report(reportItem, "");
			System.out.println(esa.toShortString());
		}
		finally
		{
			FileUtils.deleteQuietly(compactWomA);
			FileUtils.deleteQuietly(compactWomB);
		}
	}
}
