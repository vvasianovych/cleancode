/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.Comparator;

import com.vectrace.MercurialEclipse.model.ChangeSet;

public class ChangeSetComparator implements Comparator<ChangeSet> {

	public int compare(ChangeSet o1, ChangeSet o2) {
		int result = o2.getChangesetIndex() - o1.getChangesetIndex();

		// we need to cover the situation when repo-indices are the same
		if (result == 0 && o1.getDateString() != null && o2.getDateString() != null) {
			int dateCompare = o2.getRealDate().compareTo(o1.getRealDate());
			if (dateCompare != 0) {
				result = dateCompare;
			}
		}

		return result;
	}
}