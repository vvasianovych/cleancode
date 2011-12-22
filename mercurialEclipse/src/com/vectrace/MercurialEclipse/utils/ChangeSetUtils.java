/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Philip Graf   implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;

/**
 * @author Philip Graf
 */
public final class ChangeSetUtils {

	private ChangeSetUtils() {
		// hide constructor of utility class.
	}

	/**
	 * Returns a printable String listing of all tags of a changeset. If the changeset does not have
	 * any tags, an empty String is returned.
	 *
	 * @param changeSet
	 *            The changeset. May be {@code null}.
	 * @return a printable String listing all tags of a changeset. Never returns {@code null}.
	 */
	public static String getPrintableTagsString(ChangeSet changeSet) {
		String tagsString = ""; //$NON-NLS-1$

		if (changeSet != null) {
			Tag[] tags = changeSet.getTags();
			if (tags.length > 0) {
				StringBuilder builder = new StringBuilder(tags[0].getName());
				for (int i = 1; i < tags.length; i++) {
					builder.append(", ").append(tags[i].getName()); //$NON-NLS-1$
				}
				tagsString = builder.toString();
			}
		}

		return tagsString;
	}

	/**
	 * Returns the changeset index followed by the short revision. If the changeset is {@code null},
	 * an empty String is returned. If the short revision is not available, only the index is
	 * returned.
	 *
	 *@param changeSet
	 *            The changeset. May be {@code null}.
	 * @return the changeset index followed by the short revision. Never returns {@code null}.
	 */
	public static String getPrintableRevisionShort(ChangeSet changeSet) {
		String revisionShort;

		if (changeSet == null) {
			revisionShort = ""; //$NON-NLS-1$

		} else if (changeSet.getNodeShort() == null) {
			revisionShort = Integer.toString(changeSet.getChangesetIndex());

		} else {
			revisionShort = changeSet.getChangesetIndex() + ":" + changeSet.getNodeShort(); //$NON-NLS-1$
		}

		return revisionShort;
	}

}
