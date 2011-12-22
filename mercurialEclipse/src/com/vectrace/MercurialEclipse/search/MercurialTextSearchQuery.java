/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Juerg Billeter, juergbi@ethz.ch - 47136 Search view should show match objects
 *     Ulrich Etter, etteru@ethz.ch - 47136 Search view should show match objects
 *     Roman Fuchs, fuchsro@ethz.ch - 47136 Search view should show match objects
 *     Bastian Doetsch - MercurialEclipse adaptation
 *     Philip Graf - Fixed bugs which FindBugs found
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.text.SearchResultUpdater;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;

@SuppressWarnings("restriction")
public class MercurialTextSearchQuery implements ISearchQuery {

	private static final class TextSearchResultCollector extends
			TextSearchRequestor {

		private final AbstractTextSearchResult fResult;
		private final boolean fIsFileSearchOnly;
		private final boolean fSearchInBinaries;
		private ArrayList<MercurialMatch> fCachedMatches;

		private TextSearchResultCollector(AbstractTextSearchResult result,
				boolean isFileSearchOnly, boolean searchInBinaries) {
			fResult = result;
			fIsFileSearchOnly = isFileSearchOnly;
			fSearchInBinaries = searchInBinaries;

		}

		@Override
		public boolean acceptFile(IFile file) throws CoreException {
			if (fIsFileSearchOnly) {
				fResult.addMatch(new MercurialMatch(file));
			}
			flushMatches();
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.search.core.text.TextSearchRequestor#reportBinaryFile
		 * (org.eclipse.core.resources.IFile)
		 */
		@Override
		public boolean reportBinaryFile(IFile file) {
			return fSearchInBinaries;
		}

		@Override
		public boolean acceptPatternMatch(TextSearchMatchAccess matchRequestor)
				throws CoreException {
			if (matchRequestor instanceof MercurialTextSearchMatchAccess) {
				MercurialMatch match = new MercurialMatch(
						(MercurialTextSearchMatchAccess) matchRequestor);
				fCachedMatches.add(match);
				return true;
			}
			return false;
		}

		@Override
		public void beginReporting() {
			fCachedMatches = new ArrayList<MercurialMatch>();
		}

		@Override
		public void endReporting() {
			flushMatches();
			fCachedMatches = null;
		}

		private void flushMatches() {
			if (!fCachedMatches.isEmpty()) {
				fResult.addMatches(fCachedMatches
						.toArray(new Match[fCachedMatches.size()]));
				fCachedMatches.clear();
			}
		}
	}

	private final MercurialTextSearchScope fScope;
	private final String fSearchText;

	private MercurialTextSearchResult fResult;

	public MercurialTextSearchQuery(String searchText, MercurialTextSearchScope scope) {
		fSearchText = searchText;
		fScope = scope;
	}

	public MercurialTextSearchScope getSearchScope() {
		return fScope;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public IStatus run(final IProgressMonitor monitor) {
		AbstractTextSearchResult textResult = (AbstractTextSearchResult) getSearchResult();
		textResult.removeAll();

		Pattern searchPattern = getSearchPattern();
		boolean searchInBinaries = !isScopeAllFileTypes();

		TextSearchResultCollector collector = new TextSearchResultCollector(
				textResult, isFileNameSearch(), searchInBinaries);

		TextSearchEngine engine = new MercurialTextSearchEngine();
		return engine.search(fScope, collector, searchPattern, monitor);
	}

	private boolean isScopeAllFileTypes() {
		String[] fileNamePatterns = fScope.getFileNamePatterns();
		if (fileNamePatterns == null) {
			return true;
		}
		for (int i = 0; i < fileNamePatterns.length; i++) {
			if ("*".equals(fileNamePatterns[i])) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	public String getLabel() {
		return "Mercurial Text Search for " + fSearchText;
	}

	public String getSearchString() {
		return fSearchText;
	}

	public String getResultLabel(int nMatches) {
		String searchString = getSearchString();
		if (searchString.length() > 0) {
			// text search
			if (isScopeAllFileTypes()) {
				// search all file extensions
				if (nMatches == 1) {
					Object[] args = { searchString, fScope.getDescription() };
					return Messages.format(
							SearchMessages.FileSearchQuery_singularLabel, args);
				}
				Object[] args = { searchString, Integer.valueOf(nMatches),
						fScope.getDescription() };
				return Messages.format(
						SearchMessages.FileSearchQuery_pluralPattern, args);
			}
			// search selected file extensions
			if (nMatches == 1) {
				Object[] args = { searchString, fScope.getDescription(),
						fScope.getFilterDescription() };
				return Messages
						.format(
								SearchMessages.FileSearchQuery_singularPatternWithFileExt,
								args);
			}
			Object[] args = { searchString, Integer.valueOf(nMatches),
					fScope.getDescription(), fScope.getFilterDescription() };
			return Messages.format(
					SearchMessages.FileSearchQuery_pluralPatternWithFileExt,
					args);
		}
		// file search
		if (nMatches == 1) {
			Object[] args = { fScope.getFilterDescription(),
					fScope.getDescription() };
			return Messages
					.format(
							SearchMessages.FileSearchQuery_singularLabel_fileNameSearch,
							args);
		}
		Object[] args = { fScope.getFilterDescription(), Integer.valueOf(nMatches),
				fScope.getDescription() };
		return Messages.format(
				SearchMessages.FileSearchQuery_pluralPattern_fileNameSearch,
				args);
	}

	/**
	 * @param result
	 *            all result are added to this search result
	 * @param monitor
	 *            the progress monitor to use
	 * @param file
	 *            the file to search in
	 * @return returns the status of the operation
	 */
	public IStatus searchInFile(final AbstractTextSearchResult result,
			final IProgressMonitor monitor, IFile file) {
		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(
				new IResource[] { file }, new String[] { "*" }, true); //$NON-NLS-1$

		Pattern searchPattern = getSearchPattern();
		TextSearchResultCollector collector = new TextSearchResultCollector(
				result, isFileNameSearch(), true);

		return new MercurialTextSearchEngine().search(scope, collector,
				searchPattern, monitor);
	}

	protected Pattern getSearchPattern() {
		return PatternConstructor.createPattern(fSearchText, true,
				true);
	}

	public boolean isFileNameSearch() {
		return fSearchText.length() == 0;
	}

	public boolean canRerun() {
		return true;
	}

	public ISearchResult getSearchResult() {
		if (fResult == null) {
			fResult = new MercurialTextSearchResult(this);
			new SearchResultUpdater(fResult);
		}
		return fResult;
	}
}
