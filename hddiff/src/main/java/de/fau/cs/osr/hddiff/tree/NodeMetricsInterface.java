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
package de.fau.cs.osr.hddiff.tree;

public interface NodeMetricsInterface
{
	/**
	 * Computes the hash for a single node (no recursion!). It's the caller's
	 * responsibility to combine a node's hash with the hash values of its
	 * children to create a subtree hash.
	 */
	int computeHash(DiffNode node);
	
	int computeWeight(DiffNode node);
	
	boolean verifyHashEquality(DiffNode n1, DiffNode n2);
}
