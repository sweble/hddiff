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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import de.fau.cs.osr.hddiff.utils.ReportItem.Indicator;
import de.fau.cs.osr.hddiff.utils.ReportItem.IndicatorNumber;

public class Report
{
	Set<String> headers = new HashSet<>();
	
	Map<String, String> units = new HashMap<>();
	
	LinkedList<ReportItem> items = new LinkedList<>();
	
	// =========================================================================
	
	public void add(ReportItem item)
	{
		items.add(item);
		Map<String, Indicator> indicators = item.getIndicators();
		headers.addAll(indicators.keySet());
		for (Entry<String, Indicator> e : indicators.entrySet())
		{
			if (e.getValue() instanceof IndicatorNumber)
				units.put(e.getKey(), ((IndicatorNumber) e.getValue()).getUnit());
		}
	}
	
	public void writeCsv(File outFile, Locale locale, String encoding) throws IOException
	{
		try (OutputStream os = new FileOutputStream(outFile))
		{
			try (PrintStream ps = new PrintStream(os, true, encoding))
			{
				ArrayList<String> headers = new ArrayList<String>(this.headers);
				Collections.sort(headers);
				int cols = headers.size();
				
				int i = 0;
				for (String header : headers)
				{
					ps.print(StringEscapeUtils.escapeCsv(header));
					if (++i < cols)
						ps.print(',');
				}
				ps.println();
				
				i = 0;
				for (String header : headers)
				{
					String unit = units.get(header);
					if (unit != null)
						ps.print(StringEscapeUtils.escapeCsv(unit));
					if (++i < cols)
						ps.print(',');
				}
				ps.println();
				
				for (ReportItem item : items)
				{
					Map<String, Indicator> values = item.getIndicators();
					
					i = 0;
					for (String header : headers)
					{
						Indicator ind = values.get(header);
						if (ind != null)
							ps.print(StringEscapeUtils.escapeCsv(ind.formatValue(locale)));
						if (++i < cols)
							ps.print(',');
					}
					ps.println();
				}
			}
		}
	}
}
