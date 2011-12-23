/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.ui.IWorkingSet;

import com.vectrace.MercurialEclipse.search.MercurialTextSearchPage.TextSearchPageInput;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchQueryProvider {

	private final boolean all;

	public MercurialTextSearchQueryProvider(boolean all) {
		this.all = all;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.eclipse.search.ui.text.TextSearchQueryProvider#createQuery( TextSearchInput)
	 */
	public ISearchQuery createQuery(TextSearchPageInput input) {
		MercurialTextSearchScope scope = input.getScope();
		String text = input.getSearchText();
		return new MercurialTextSearchQuery(text, scope);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang .String)
	 */
	public ISearchQuery createQuery(String searchForString) {
		MercurialTextSearchScope scope = MercurialTextSearchScope.newWorkspaceScope(
				getPreviousFileNamePatterns(), all);
		return new MercurialTextSearchQuery(searchForString, scope);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang .String,
	 * org.eclipse.core.resources.IResource[])
	 */
	public ISearchQuery createQuery(String selectedText, IResource[] resources) {
		MercurialTextSearchScope scope = MercurialTextSearchScope.newSearchScope(resources,
				getPreviousFileNamePatterns(), all);
		return new MercurialTextSearchQuery(selectedText, scope);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.text.TextSearchQueryProvider#createQuery(java.lang .String,
	 * org.eclipse.ui.IWorkingSet[])
	 */
	public ISearchQuery createQuery(String selectedText, IWorkingSet[] ws) {
		MercurialTextSearchScope scope = MercurialTextSearchScope.newSearchScope(ws,
				getPreviousFileNamePatterns(), all);
		return new MercurialTextSearchQuery(selectedText, scope);
	}

	private String[] getPreviousFileNamePatterns() {
		return new String[] { "*" }; //$NON-NLS-1$
	}
}
