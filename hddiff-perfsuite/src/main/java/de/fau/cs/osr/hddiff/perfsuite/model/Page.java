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
package de.fau.cs.osr.hddiff.perfsuite.model;

public class Page
{
	private java.lang.Long id;
	
	private java.lang.Integer namespace;
	
	private java.lang.String title;
	
	private java.lang.String redirectTitle;
	
	private java.lang.Long currentRevisionId;
	
	public Page()
	{
	}
	
	public Page(
			java.lang.Long id,
			java.lang.Integer namespace,
			java.lang.String title,
			java.lang.String redirectTitle,
			java.lang.Long currentRevisionId)
	{
		this.id = id;
		this.namespace = namespace;
		this.title = title;
		this.redirectTitle = redirectTitle;
		this.currentRevisionId = currentRevisionId;
	}
	
	public java.lang.Long getId()
	{
		return this.id;
	}
	
	public void setId(java.lang.Long id)
	{
		this.id = id;
	}
	
	public java.lang.Integer getNamespace()
	{
		return this.namespace;
	}
	
	public void setNamespace(java.lang.Integer namespace)
	{
		this.namespace = namespace;
	}
	
	public java.lang.String getTitle()
	{
		return this.title;
	}
	
	public void setTitle(java.lang.String title)
	{
		this.title = title;
	}
	
	public java.lang.String getRedirectTitle()
	{
		return this.redirectTitle;
	}
	
	public void setRedirectTitle(java.lang.String redirectTitle)
	{
		this.redirectTitle = redirectTitle;
	}
	
	public java.lang.Long getCurrentRevisionId()
	{
		return this.currentRevisionId;
	}
	
	public void setCurrentRevisionId(java.lang.Long currentRevisionId)
	{
		this.currentRevisionId = currentRevisionId;
	}
}
