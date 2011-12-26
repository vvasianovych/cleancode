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

import org.eclipse.jface.viewers.ViewerSorter;

import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

public class RepositorySorter extends ViewerSorter {

	private static final int REPO_ROOT_CATEGORY = 1;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
	 */
	@Override
	public int category(Object element) {
		if (element instanceof IHgRepositoryLocation) {
			return REPO_ROOT_CATEGORY;
		}
		return 0;
	}
}
