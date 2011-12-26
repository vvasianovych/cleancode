/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

public class HgRevision {
	public static final HgRevision TIP = new HgRevision("tip"); //$NON-NLS-1$
	private final String changeset;
	private final int revision;

	protected HgRevision(String changeset) {
		this(changeset, -1);
	}

	public HgRevision(String changeset, int revision) {
		this.changeset = changeset;
		this.revision = revision;
	}

	public String getChangeset() {
		return changeset;
	}

	public int getRevision() {
		return revision;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HgRevision) {
			HgRevision r = (HgRevision) obj;
			return r.revision == revision || r.changeset.equals(changeset);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return changeset.hashCode();
	}

	@Override
	public String toString() {
		return revision + ":" + changeset; //$NON-NLS-1$
	}

	public static HgRevision parse(String s) {
		int i = s.indexOf(':');
		return new HgRevision(s.substring(i + 1), Integer.parseInt(s.substring(
				0, i)));
	}
}
