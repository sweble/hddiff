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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ReportItem
{
	public static final int LOG_LEVEL_TRACE = 600;
	
	public static final int LOG_LEVEL_DEBUG = 500;
	
	public static final int LOG_LEVEL_INFO = 400;
	
	public static final int LOG_LEVEL_WARNING = 300;
	
	public static final int LOG_LEVEL_ERROR = 200;
	
	public static final int LOG_LEVEL_FATAL = 100;
	
	// =========================================================================
	
	private final Map<String, Indicator> indicators;
	
	private final List<Message> messages;
	
	private final String prefix;
	
	private int logLevel;
	
	// =========================================================================
	
	public ReportItem()
	{
		indicators = new LinkedHashMap<>();
		messages = new LinkedList<>();
		logLevel = 0;
		prefix = "";
	}
	
	public ReportItem(int level)
	{
		this();
		this.logLevel = level;
	}
	
	public ReportItem(String prefix, ReportItem reportItem)
	{
		this.indicators = reportItem.indicators;
		this.messages = reportItem.messages;
		this.logLevel = reportItem.logLevel;
		this.prefix = prefix + reportItem.prefix;
	}
	
	// =========================================================================
	
	public Map<String, Indicator> getIndicators()
	{
		return indicators;
	}
	
	public List<Message> getMessages()
	{
		return messages;
	}
	
	// =========================================================================
	
	public void setLogLevel(int logLevel)
	{
		this.logLevel = logLevel;
	}
	
	public int getLogLevel()
	{
		return logLevel;
	}
	
	public boolean isDebugEnabled()
	{
		return accept(LOG_LEVEL_DEBUG);
	}
	
	public boolean isInfoEnabled()
	{
		return accept(LOG_LEVEL_INFO);
	}
	
	public boolean isWarningEnabled()
	{
		return accept(LOG_LEVEL_WARNING);
	}
	
	private boolean accept(Severity severity)
	{
		int level = severity.getLevel();
		return accept(level);
	}
	
	private boolean accept(int level)
	{
		return logLevel >= level;
	}
	
	// =========================================================================
	
	public Timer startTimer(String name)
	{
		return new Timer(name, System.nanoTime());
	}
	
	public void recordFigure(String name, Number value, String unit)
	{
		indicators.put(prefix + name, new IndicatorNumber(name, value, unit));
	}
	
	public void recordText(String name, String text)
	{
		indicators.put(prefix + name, new IndicatorText(name, text));
	}
	
	// =========================================================================
	
	public void debug(String message, Object... values)
	{
		recordMessage(Severity.DEBUG, prefix + message, values);
	}
	
	public void info(String message, Object... values)
	{
		recordMessage(Severity.INFO, prefix + message, values);
	}
	
	public void warn(String message, Object... values)
	{
		recordMessage(Severity.WARNING, prefix + message, values);
	}
	
	public void recordMessage(
			Severity severity,
			String message,
			Object... values)
	{
		try
		{
			if (accept(severity))
				messages.add(new Message(severity, String.format(message, values)));
		}
		catch (IllegalFormatException e)
		{
			messages.add(new Message(severity, String.format(
					"Failed to format message: \"%s\" with values: %s",
					message,
					Arrays.toString(values))));
		}
	}
	
	// =========================================================================
	
	@Override
	public String toString()
	{
		ArrayList<Indicator> indicators = new ArrayList<>(this.indicators.values());
		Collections.sort(indicators);
		return String.format("" +
				"Report:\n"
				+ "  Indicators:\n"
				+ "    %s\n"
				+ "  Messages:\n"
				+ "    %s",
				StringUtils.join(indicators, "\n    "),
				StringUtils.join(messages, "\n    "));
	}
	
	// =========================================================================
	
	public static abstract class Indicator
			implements
				Comparable<Indicator>
	{
		
		private final String name;
		
		private Indicator(String name)
		{
			if (name == null)
				throw new IllegalArgumentException();
			this.name = name;
		}
		
		public String getName()
		{
			return name;
		}
		
		@Override
		public int compareTo(Indicator o)
		{
			return name.compareTo(o.name);
		}
		
		@Override
		public String toString()
		{
			return "Indicator{" + "name=" + getName() + '}';
		}
		
		public abstract String formatValue(Locale locale);
	}
	
	// =========================================================================
	
	public static final class IndicatorNumber
			extends
				Indicator
	{
		private final Number value;
		
		private final String unit;
		
		public IndicatorNumber(String name, Number value, String unit)
		{
			super(name);
			if (value == null || unit == null)
				throw new IllegalArgumentException();
			this.value = value;
			this.unit = unit;
		}
		
		public Number getValue()
		{
			return value;
		}
		
		public String getUnit()
		{
			return unit;
		}
		
		public boolean isFloatingPoint()
		{
			return (value instanceof Float) ||
					(value instanceof Double) ||
					(value instanceof BigDecimal);
		}
		
		public String getNameWithUnit()
		{
			return getName() + " (" + getUnit() + ")";
		}
		
		@Override
		public String toString()
		{
			if (isFloatingPoint())
				return String.format("%s = %.3g %s", getName(), getValue().doubleValue(), getUnit());
			else
				return String.format("%s = %d %s", getName(), getValue().longValue(), getUnit());
		}
		
		@Override
		public String formatValue(Locale locale)
		{
			if (isFloatingPoint())
				return String.format(locale, "%g", getValue().doubleValue());
			else
				return String.format(locale, "%d", getValue().longValue());
		}
	}
	
	// =========================================================================
	
	public static final class IndicatorText
			extends
				Indicator
	{
		private final String text;
		
		public IndicatorText(String name, String text)
		{
			super(name);
			if (text == null)
				throw new IllegalArgumentException();
			this.text = text;
		}
		
		public String getText()
		{
			return text;
		}
		
		@Override
		public String toString()
		{
			return String.format("%s = %s", getName(), getText());
		}
		
		@Override
		public String formatValue(Locale locale)
		{
			return getText();
		}
	}
	
	// =========================================================================
	
	public static final class Message
	{
		
		private final String message;
		
		private final Severity severity;
		
		private Message(Severity severity, String message)
		{
			this.severity = severity;
			this.message = message;
		}
		
		public String getMessage()
		{
			return message;
		}
		
		public Severity getSeverity()
		{
			return severity;
		}
		
		@Override
		public String toString()
		{
			return "Message{" + "message=" + message + ", severity=" + severity + '}';
		}
	}
	
	// =========================================================================
	
	public static enum Severity
	{
		TRACE
		{
			@Override
			public int getLevel()
			{
				return LOG_LEVEL_TRACE;
			}
		},
		DEBUG
		{
			@Override
			public int getLevel()
			{
				return LOG_LEVEL_DEBUG;
			}
		},
		INFO
		{
			@Override
			public int getLevel()
			{
				return LOG_LEVEL_INFO;
			}
		},
		WARNING
		{
			@Override
			public int getLevel()
			{
				return LOG_LEVEL_WARNING;
			}
		},
		ERROR
		{
			@Override
			public int getLevel()
			{
				return LOG_LEVEL_ERROR;
			}
		};
		
		public abstract int getLevel();
	}
	
	// =========================================================================
	
	public final class Timer
	{
		
		private final long start;
		
		private final String name;
		
		private Timer(String name, long nanoTime)
		{
			this.name = name;
			this.start = nanoTime;
		}
		
		public double stop()
		{
			long stop = System.nanoTime();
			double delta = (stop - start) / (1000. * 1000. * 1000.);
			ReportItem.this.recordFigure(name, delta, "s");
			return delta;
		}
	}
}
