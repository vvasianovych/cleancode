/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.runtime.Assert;

public class Bookmark {

	/** name of the branch, unique in the repository */
	private final String name;
	private final int revision;
	private final String shortNode;
	private final boolean active;

	public Bookmark(String name, int revision, String globalId, boolean active) {
		super();
		this.name = name;
		this.revision = revision;
		this.shortNode = globalId;
		this.active = active;
	}

	public Bookmark(String line) {
		Assert.isTrue(line.length() > 0);
		Assert.isTrue(!line.startsWith("no bookmarks set"));         //$NON-NLS-1$
		active = line.startsWith(" *"); //$NON-NLS-1$
		int lastSpace = line.lastIndexOf(" "); //$NON-NLS-1$
		name = line.substring(3, lastSpace).trim();
		int colon = line.lastIndexOf(":"); //$NON-NLS-1$
		revision = Integer.parseInt(line.substring(lastSpace + 1, colon));
		shortNode = line.substring(colon + 1);
	}

	public String getName() {
		return name;
	}

	public int getRevision() {
		return revision;
	}

	public String getShortNodeId() {
		return shortNode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Bookmark other = (Bookmark) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

}
