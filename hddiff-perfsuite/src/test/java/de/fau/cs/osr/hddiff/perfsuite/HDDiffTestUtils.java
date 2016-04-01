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
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.sweble.wikitext.engine.ExpansionCallback;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.utils.WtEngineToolbox.TestExpansionCallback;
import org.sweble.wom3.Wom3Document;
import org.sweble.wom3.Wom3Node;
import org.sweble.wom3.swcadapter.utils.WtWom3Toolbox;

import de.fau.cs.osr.hddiff.HDDiff;
import de.fau.cs.osr.hddiff.HDDiffOptions;
import de.fau.cs.osr.hddiff.editscript.EditOp;
import de.fau.cs.osr.hddiff.editscript.EditScriptManager;
import de.fau.cs.osr.hddiff.perfsuite.model.Page;
import de.fau.cs.osr.hddiff.perfsuite.model.Revision;
import de.fau.cs.osr.hddiff.perfsuite.util.PageRevDiffNode;
import de.fau.cs.osr.hddiff.tree.DiffNode;
import de.fau.cs.osr.hddiff.utils.ReportItem;
import de.fau.cs.osr.hddiff.utils.WordSubstringJudge;
import de.fau.cs.osr.hddiff.wom.WomNodeEligibilityTester;
import de.fau.cs.osr.hddiff.wom.WomNodeMetrics;
import de.fau.cs.osr.hddiff.wom.WomToDiffNodeConverter;
import de.fau.cs.osr.utils.ComparisonException;
import de.fau.cs.osr.utils.TestResourcesFixture;

public class HDDiffTestUtils
{
	protected static TestResourcesFixture getTestResourcesFixture() throws FileNotFoundException
	{
		File path = TestResourcesFixture.resourceNameToFile(
				HDDiffTestUtils.class, "/");

		return new TestResourcesFixture(path);
	}

	// =========================================================================

	private final WtWom3Toolbox wtWom3Toolbox;

	// =========================================================================

	protected HDDiffTestUtils()
	{
		wtWom3Toolbox = new WtWom3Toolbox();

		// For testing we don't want stuff to be removed
		getWtWom3Toolbox().getWikiConfig().getEngineConfig()
				.setTrimTransparentBeforeParsing(false);
	}

	// =========================================================================

	public WtWom3Toolbox getWtWom3Toolbox()
	{
		return wtWom3Toolbox;
	}

	// =========================================================================

	public List<EditOp> generateDiff(
			File inputFileA,
			String inputSubDir) throws Exception
	{
		ExpansionCallback callback = new TestExpansionCallback();

		Wom3Document womA = parse(inputFileA, callback);

		File inputFileB = aToB(inputFileA);
		Wom3Document womB = parse(inputFileB, callback);

		return generateDiff(womA, womB, inputFileA.getName());
	}

	public List<EditOp> generateAndApplyDiff(
			File inputFileA,
			String inputSubDir) throws Exception
	{
		ExpansionCallback callback = new TestExpansionCallback();

		Wom3Document womA = parse(inputFileA, callback);

		File inputFileB = aToB(inputFileA);
		Wom3Document womB = parse(inputFileB, callback);

		return generateAndApplyDiff(womA, womB, inputFileA.getName());
	}

	public Wom3Document parse(File inputFile, ExpansionCallback callback) throws Exception
	{
		String fileTitle = FilenameUtils.getBaseName(inputFile.getName());

		PageId pageId = getWtWom3Toolbox().makePageId(fileTitle);

		Wom3Document wom = getWtWom3Toolbox().wmToWom(inputFile, pageId, callback, "UTF8").womDoc;

		// We will modify this document in possibly illegal ways later
		wom.setStrictErrorChecking(false);

		return wom;
	}

	protected static File aToB(File inputFileA)
	{
		return new File(inputFileA.getPath().replaceAll(
				"\\.a\\.wikitext$",
				".b.wikitext"));
	}

	protected static List<EditOp> generateAndApplyDiff(
			Wom3Document womA,
			Wom3Document womB,
			String title) throws ComparisonException
	{
		HDDiffOptions options = setupHDDiff();
		options.setDumpTreesFileTitle(title);

		ReportItem report = new ReportItem();
		try
		{
			Wom3Node rootA = (Wom3Node) womA.getDocumentElement();
			Wom3Node rootB = (Wom3Node) womB.getDocumentElement();

			DiffNode root1 = WomToDiffNodeConverter.preprocess(rootA);
			DiffNode root2 = WomToDiffNodeConverter.preprocess(rootB);

			List<EditOp> es = HDDiff.editScript(root1, root2, options, report);
			System.out.println(new EditScriptAnalysis(es).toShortString());

			applyEditScript(es);

			root1.compareNativeDeep(root2);

			return es;
		}
		finally
		{
			System.out.println(report.toString());
		}
	}

	protected static List<EditOp> generateDiff(
			Wom3Document womA,
			Wom3Document womB,
			String title) throws ComparisonException
	{
		HDDiffOptions options = setupHDDiff();
		options.setDumpTreesFileTitle(title);

		Wom3Node rootA = (Wom3Node) womA.getDocumentElement();
		Wom3Node rootB = (Wom3Node) womB.getDocumentElement();

		DiffNode root1 = WomToDiffNodeConverter.preprocess(rootA);
		DiffNode root2 = WomToDiffNodeConverter.preprocess(rootB);

		return HDDiff.editScript(root1, root2, options, null);
	}

	protected static HDDiffOptions setupHDDiff()
	{
		HDDiffOptions options = new HDDiffOptions();

		options.setNodeMetrics(new WomNodeMetrics());

		options.setMinSubtreeWeight(12);

		options.setEnableTnsm(true);
		options.setTnsmEligibilityTester(new WomNodeEligibilityTester());
		options.setTnsmSubstringJudge(new WordSubstringJudge(8, 3));

		return options;
	}

	protected static void applyEditScript(List<EditOp> es)
	{
		new EditScriptManager(es).apply();
	}

	protected static PageRevDiffNode makePageRevDiffNode(
			File inputFileA,
			DiffNode root1,
			DiffNode root2)
	{
		Page page = new Page();
		page.setId(1L);
		page.setTitle(inputFileA.getName());
		Revision revA = new Revision();
		revA.setId(2L);
		Revision revB = new Revision();
		revB.setId(3L);
		PageRevDiffNode prdn = new PageRevDiffNode(page, revA, revB, root1, root2);
		return prdn;
	}
}
