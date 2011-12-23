/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;

/**
 * @author Andrei
 */
public class ChangesetGroup {

	private final Direction direction;
	private final String name;
	private final Set<ChangeSet> changesets;

	public ChangesetGroup(String name, Direction direction) {
		this.name = name;
		this.direction = direction;
		// CopyOnWriteArraySet to prevent ConcurrentModificationException.
		this.changesets = new CopyOnWriteArraySet<ChangeSet>();
	}

	public Direction getDirection() {
		return direction;
	}

	public String getName() {
		return name;
	}

	public Set<ChangeSet> getChangesets() {
		return changesets;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChangesetGroup [");
		if (direction != null) {
			builder.append("direction=");
			builder.append(direction);
			builder.append(", ");
		}
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (changesets != null) {
			builder.append("changesets=");
			builder.append(changesets);
		}
		builder.append("]");
		return builder.toString();
	}
}
