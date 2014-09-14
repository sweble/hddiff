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

public class Revision
{
	private java.lang.Long id;
	
	private java.lang.Long parentId;
	
	private java.lang.Long pageId;
	
	private java.lang.Long contributorId;
	
	private java.lang.String contributorIp;
	
	private java.sql.Timestamp timestamp;
	
	private java.lang.Boolean minor;
	
	private java.lang.String comment;
	
	private java.lang.Boolean commentDeleted;
	
	private byte[] textSha1;
	
	private java.lang.Boolean textDeleted;
	
	private java.lang.String model;
	
	private java.lang.String format;
	
	public Revision()
	{
	}
	
	public Revision(
			java.lang.Long id,
			java.lang.Long parentId,
			java.lang.Long pageId,
			java.lang.Long contributorId,
			java.lang.String contributorIp,
			java.sql.Timestamp timestamp,
			java.lang.Boolean minor,
			java.lang.String comment,
			java.lang.Boolean commentDeleted,
			byte[] textSha1,
			java.lang.Boolean textDeleted,
			java.lang.String model,
			java.lang.String format)
	{
		this.id = id;
		this.parentId = parentId;
		this.pageId = pageId;
		this.contributorId = contributorId;
		this.contributorIp = contributorIp;
		this.timestamp = timestamp;
		this.minor = minor;
		this.comment = comment;
		this.commentDeleted = commentDeleted;
		this.textSha1 = textSha1;
		this.textDeleted = textDeleted;
		this.model = model;
		this.format = format;
	}
	
	public java.lang.Long getId()
	{
		return this.id;
	}
	
	public void setId(java.lang.Long id)
	{
		this.id = id;
	}
	
	public java.lang.Long getParentId()
	{
		return this.parentId;
	}
	
	public void setParentId(java.lang.Long parentId)
	{
		this.parentId = parentId;
	}
	
	public java.lang.Long getPageId()
	{
		return this.pageId;
	}
	
	public void setPageId(java.lang.Long pageId)
	{
		this.pageId = pageId;
	}
	
	public java.lang.Long getContributorId()
	{
		return this.contributorId;
	}
	
	public void setContributorId(java.lang.Long contributorId)
	{
		this.contributorId = contributorId;
	}
	
	public java.lang.String getContributorIp()
	{
		return this.contributorIp;
	}
	
	public void setContributorIp(java.lang.String contributorIp)
	{
		this.contributorIp = contributorIp;
	}
	
	public java.sql.Timestamp getTimestamp()
	{
		return this.timestamp;
	}
	
	public void setTimestamp(java.sql.Timestamp timestamp)
	{
		this.timestamp = timestamp;
	}
	
	public java.lang.Boolean getMinor()
	{
		return this.minor;
	}
	
	public void setMinor(java.lang.Boolean minor)
	{
		this.minor = minor;
	}
	
	public java.lang.String getComment()
	{
		return this.comment;
	}
	
	public void setComment(java.lang.String comment)
	{
		this.comment = comment;
	}
	
	public java.lang.Boolean getCommentDeleted()
	{
		return this.commentDeleted;
	}
	
	public void setCommentDeleted(java.lang.Boolean commentDeleted)
	{
		this.commentDeleted = commentDeleted;
	}
	
	public byte[] getTextSha1()
	{
		return this.textSha1;
	}
	
	public void setTextSha1(byte[] textSha1)
	{
		this.textSha1 = textSha1;
	}
	
	public java.lang.Boolean getTextDeleted()
	{
		return this.textDeleted;
	}
	
	public void setTextDeleted(java.lang.Boolean textDeleted)
	{
		this.textDeleted = textDeleted;
	}
	
	public java.lang.String getModel()
	{
		return this.model;
	}
	
	public void setModel(java.lang.String model)
	{
		this.model = model;
	}
	
	public java.lang.String getFormat()
	{
		return this.format;
	}
	
	public void setFormat(java.lang.String format)
	{
		this.format = format;
	}
}
