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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import de.fau.cs.osr.hddiff.editscript.EditOp;
import de.fau.cs.osr.utils.NamedParametrized;
import de.fau.cs.osr.utils.SimpleConsoleOutput;
import de.fau.cs.osr.utils.TestResourcesFixture;

/**
 * Tests if conversion from Wikitext to WOM (using an AST as intermediate
 * representation) works for all basic elements of Wikitext. The parsed Wikitext
 * is expanded. Some elements are also tested with an unexpanded AST.
 */
@RunWith(value = NamedParametrized.class)
public class HDDiffTest
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
	
	// =========================================================================
	
	public HDDiffTest(
			String title,
			TestResourcesFixture resources,
			File inputFile)
	{
		this.inputFile = inputFile;
	}
	
	// =========================================================================
	
	@Test
	public void testFindAGoodNameForThisTest() throws Exception
	{
		// TODO: Do some actual testing...
		SimpleConsoleOutput.printBigSep(inputFile.toString());
		List<EditOp> es = utils.generateAndApplyDiff(inputFile, INPUT_SUB_DIR);
		
		// ...
	}
}
