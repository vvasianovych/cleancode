/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian	implementation
 * 		Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.internal.ui.SearchPluginImages;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {

	private MercurialTextSearchQuery query;

	public MercurialTextSearchResult() {
		super();
	}

	public MercurialTextSearchResult(MercurialTextSearchQuery query) {
		this.query = query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return this;
	}

	@SuppressWarnings("restriction")
	public ImageDescriptor getImageDescriptor() {
		return SearchPluginImages.DESC_OBJ_TSEARCH_DPDN;
	}

	public String getLabel() {
		return query.getResultLabel(super.getMatchCount());
	}

	public ISearchQuery getQuery() {
		return this.query;
	}

	public String getTooltip() {
		return getLabel();
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		return new Match[0];
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		return new Match[0];
	}

	public IFile getFile(Object element) {
		if (element instanceof IFile) {
			return (IFile)element;
		}
		return null;
	}

	public boolean isShownInEditor(Match match, IEditorPart editor) {
		return false;
	}


}
