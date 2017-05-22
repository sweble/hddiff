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
package de.fau.cs.osr.hddiff.editscript;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import de.fau.cs.osr.hddiff.tree.DiffNode;

public class EditScriptManager
{
	private static final boolean ASSERTIONS = true;

	private final List<EditOp> editScript;

	private final HashMap<DiffNode, Effect> effects;

	// =========================================================================

	public EditScriptManager(List<EditOp> editScript)
	{
		this.editScript = editScript;
		this.effects = new HashMap<>(editScript.size());
		parse();
	}

	// =========================================================================

	private void parse()
	{
		for (EditOp eo : editScript)
		{
			switch (eo.getType())
			{
				case DELETE:
					addMapping((EditOpDelete) eo);
					break;
				case INSERT:
					addMapping((EditOpInsert) eo);
					break;
				case MOVE:
					EditOpMove mov = (EditOpMove) eo;
					addMapping(mov);
					break;
				case UPDATE:
					addMapping((EditOpUpdate) eo);
					break;
				case SPLIT:
					// Splits were already applied during diffing.
					break;
			}
		}
	}

	private void addMapping(EditOpUpdate upd)
	{
		Effect e = addEffect(upd.getUpdatedNode());
		e.setIsUpdated(upd);
	}

	private void addMapping(EditOpMove mov)
	{
		Effect e = addEffect(mov.getToParent());
		e.addNewChild(mov);

		e = addEffect(mov.getMovedNode());
		e.setIsMoved(mov);
	}

	private void addMapping(EditOpInsert ins)
	{
		Effect e = addEffect(ins.getParent());
		e.addNewChild(ins);

		e = addEffect(ins.getInsertedNode());
		e.setIsInserted(ins);
	}

	private void addMapping(EditOpDelete del)
	{
		Effect e = addEffect(del.getDeletedNode());
		e.setIsDeleted(del);
	}

	private Effect addEffect(DiffNode key)
	{
		Effect l = effects.get(key);
		if (l == null)
			effects.put(key, l = new Effect());
		return l;
	}

	// =========================================================================

	public void apply()
	{
		processRemoves();
		processNonRemoves();
	}

	private void processRemoves()
	{
		ListIterator<EditOp> i = editScript.listIterator(editScript.size());
		while (i.hasPrevious())
		{
			EditOp op = i.previous();
			switch (op.getType())
			{
				case DELETE:
					EditOpDelete del = (EditOpDelete) op;
					del.getDeletedNode().removeFromParent();
					break;

				case MOVE:
					EditOpMove mov = (EditOpMove) op;
					mov.getMovedNode().removeFromParent();
					break;

				default:
					break;
			}
		}
	}

	private void processNonRemoves()
	{
		for (Entry<DiffNode, Effect> e : effects.entrySet())
		{
			DiffNode affectedParent = e.getKey();
			Effect effect = e.getValue();

			if (effect.isUpdated())
			{
				EditOpUpdate upd = effect.getUpdateOp();
				upd.getUpdatedNode().applyUpdate(upd.getUpdate());
			}

			Iterator<InsertOp> insIt = effect.getInserts().iterator();
			if (!insIt.hasNext())
				continue;

			int i = 0;
			InsertOp ins = insIt.next();
			DiffNode child = affectedParent.getFirstChild();

			L2: while ((ins != null) || (child != null))
			{
				while ((ins != null) && (ins.getFinalPosition() == i))
				{
					affectedParent.appendOrInsert(ins.getInsertedNode(), child);

					if (ASSERTIONS && (ins.getInsertedNode().indexOf() != ins.getFinalPosition()))
						throw new AssertionError();

					if (!insIt.hasNext())
						// No more inserts, no point going on...
						break L2;

					ins = insIt.next();
					++i;
				}

				if (child != null)
				{
					child = child.getNextSibling();
					++i;
				}
			}
		}
	}

	// =========================================================================

	public static final class Effect
	{
		private EditOpUpdate upd;

		private EditOpMove mov;

		private EditOpInsert ins;

		private EditOpDelete del;

		private boolean removed;

		private boolean sorted;

		private List<InsertOp> inserts = null;

		// =====================================================================

		public void setIsUpdated(EditOpUpdate upd)
		{
			this.upd = upd;
		}

		public boolean isUpdated()
		{
			return upd != null;
		}

		public EditOpUpdate getUpdateOp()
		{
			return upd;
		}

		public void setIsMoved(EditOpMove mov)
		{
			this.mov = mov;
		}

		public boolean isMoved()
		{
			return mov != null;
		}

		public EditOpMove getMoveOp()
		{
			return mov;
		}

		public void setIsInserted(EditOpInsert ins)
		{
			this.ins = ins;
		}

		public boolean isInserted()
		{
			return ins != null;
		}

		public EditOpInsert getInsertOp()
		{
			return ins;
		}

		public void setIsDeleted(EditOpDelete del)
		{
			this.del = del;
		}

		public boolean isDeleted()
		{
			return del != null;
		}

		public EditOpDelete getDeleteOp()
		{
			return del;
		}

		public void addNewChild(EditOpInsert ins)
		{
			getInsertsForWriting().add(new InsertOp(
					ins,
					ins.getInsertedNode(),
					ins.getFinalPosition()));
		}

		public void addNewChild(EditOpMove mov)
		{
			getInsertsForWriting().add(new InsertOp(
					mov,
					mov.getMovedNode(),
					mov.getFinalPosition()));
		}

		private List<InsertOp> getInsertsForWriting()
		{
			sorted = false;
			if (inserts == null)
				inserts = new LinkedList<>();
			return inserts;
		}

		public Collection<InsertOp> getInserts()
		{
			if (inserts == null)
				return Collections.emptyList();
			if (!sorted)
			{
				Collections.sort(inserts);
				sorted = true;
			}
			return inserts;
		}

		// =====================================================================

		public boolean isRemoved()
		{
			return removed;
		}

		public void setRemoved(boolean removed)
		{
			this.removed = removed;
		}
	}

	// =========================================================================

	public static final class InsertOp
			implements
				Comparable<InsertOp>
	{
		private final EditOp op;

		private final int finalPosition;

		private final DiffNode insertedNode;

		// =====================================================================

		public InsertOp(
				EditOp op,
				DiffNode insertedNode,
				int finalPosition)
		{
			this.op = op;
			this.finalPosition = finalPosition;
			this.insertedNode = insertedNode;
		}

		// =====================================================================

		public EditOp getOp()
		{
			return op;
		}

		public int getFinalPosition()
		{
			return finalPosition;
		}

		public DiffNode getInsertedNode()
		{
			return insertedNode;
		}

		@Override
		public int compareTo(InsertOp o)
		{
			return Integer.compare(finalPosition, o.finalPosition);
		}
	}
}
