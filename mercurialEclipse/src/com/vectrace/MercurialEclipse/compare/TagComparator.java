/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import java.util.Comparator;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.model.Tag;

/**
 * Thsi class can be used to compare Tags by names.
 *
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class TagComparator implements Comparator<Tag> {
	private static final String TIP = HgRevision.TIP.getChangeset();

	public int compare(Tag tag1, Tag tag2) {
		/* "tip" must be always the first in the collection */
		if (tag1 == null || tag1.getName() == null || TIP.equals(tag1.getName())) {
			return -1;
		}

		if (TIP.equals(tag2.getName())) {
			return 1;
		}

		// sort by name
		int cmp = tag1.getName().compareToIgnoreCase(tag2.getName());
		if (cmp == 0) {
			// Check it case sensitive
			cmp = tag1.getName().compareTo(tag2.getName());
		}
		return cmp;
	}
}
