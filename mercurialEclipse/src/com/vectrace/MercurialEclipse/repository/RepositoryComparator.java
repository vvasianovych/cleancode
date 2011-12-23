/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.util.Comparator;

import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

/**
 * This class allows to sort IHgRepositoryLoction's alphabetically using the URL
 * or the label (if set). The case of the strings is ignored.
 */
public class RepositoryComparator implements Comparator<IHgRepositoryLocation> {

	public int compare(IHgRepositoryLocation o1, IHgRepositoryLocation o2) {
		return o1.getLocation().compareToIgnoreCase(o2.getLocation());
	}
}