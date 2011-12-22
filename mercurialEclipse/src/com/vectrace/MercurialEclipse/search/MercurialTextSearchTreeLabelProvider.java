/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation                 - initial API and implementation
 *     Juerg Billeter, juergbi@ethz.ch - 47136 Search view should show match objects
 *     Ulrich Etter, etteru@ethz.ch    - 47136 Search view should show match objects
 *     Roman Fuchs, fuchsro@ethz.ch    - 47136 Search view should show match objects
 *     Andrei Loskutov                 - bug fixes
 *     Philip Graf                     - Fixed bugs which FindBugs found
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPluginImages;
import org.eclipse.search.internal.ui.text.BasicElementLabels;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

public class MercurialTextSearchTreeLabelProvider extends LabelProvider implements IStyledLabelProvider {

	public static final int SHOW_LABEL = 1;
	public static final int SHOW_LABEL_PATH = 2;
	public static final int SHOW_PATH_LABEL = 3;

	private static final String FG_SEPARATOR_FORMAT = "{0} - {1}"; //$NON-NLS-1$

	private static final String FG_ELLIPSES = " ... "; //$NON-NLS-1$

	private final WorkbenchLabelProvider fLabelProvider;
	private final AbstractTextSearchViewPage fPage;
	private final Comparator<MercurialMatch> fMatchComparator;

	private final Image fLineMatchImage;

	private int fOrder;

	public MercurialTextSearchTreeLabelProvider(AbstractTextSearchViewPage page, int orderFlag) {
		fLabelProvider = new WorkbenchLabelProvider();
		fOrder = orderFlag;
		fPage = page;
		fLineMatchImage = SearchPluginImages.get(SearchPluginImages.IMG_OBJ_TEXT_SEARCH_LINE);
		fMatchComparator = new Comparator<MercurialMatch>() {
			public int compare(MercurialMatch o1, MercurialMatch o2) {
				return o1.getLineNumber() - o2.getLineNumber();
			}
		};
	}

	public void setOrder(int orderFlag) {
		fOrder = orderFlag;
	}

	public int getOrder() {
		return fOrder;
	}

	@Override
	public String getText(Object object) {
		return getStyledText(object).getString();
	}

	private StyledString getColoredLabelWithCounts(Object element, StyledString coloredName) {
		AbstractTextSearchResult result = fPage.getInput();
		if (result == null) {
			return coloredName;
		}

		int matchCount = result.getMatchCount(element);
		if (matchCount <= 1) {
			return coloredName;
		}

		String countInfo = Messages.format(SearchMessages.FileLabelProvider_count_format,
				Integer.valueOf(matchCount));
		coloredName.append(' ').append(countInfo, StyledString.COUNTER_STYLER);
		return coloredName;
	}

	public StyledString getStyledText(Object element) {
		if (element instanceof MercurialRevisionStorage) {
			MercurialRevisionStorage mrs = (MercurialRevisionStorage) element;
			ChangeSet cs = mrs.getChangeSet();
			if(cs == null) {
				return new StyledString("");
			}
			return new StyledString(cs.getChangesetIndex() + " [" + cs.getAuthor() + "] ("
					+ cs.getAgeDate() + ")");
		}

		if (element instanceof MercurialMatch) {
			MercurialMatch match = (MercurialMatch) element;
			return getMercurialMatchLabel(match);
		}

		if (!(element instanceof IResource)) {
			return new StyledString(element.toString());
		}

		IResource resource = (IResource) element;
		if (!resource.exists()) {
			new StyledString(SearchMessages.FileLabelProvider_removed_resource_label);
		}

		String name = BasicElementLabels.getResourceName(resource);
		if (fOrder == SHOW_LABEL) {
			return getColoredLabelWithCounts(resource, new StyledString(name));
		}

		String pathString = BasicElementLabels.getPathLabel(resource.getParent().getFullPath(),
				false);
		if (fOrder == SHOW_LABEL_PATH) {
			StyledString str = new StyledString(name);
			String decorated = Messages.format(FG_SEPARATOR_FORMAT, new String[] { str.getString(),
					pathString });

			styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, str);
			return getColoredLabelWithCounts(resource, str);
		}

		StyledString str = new StyledString(Messages.format(FG_SEPARATOR_FORMAT, new String[] {
				pathString, name }));
		return getColoredLabelWithCounts(resource, str);
	}

	/**
	 * TODO this is a temporary copy from StyledCellLabelProvider (Eclipse 3.5) we have to keep it
	 * as long as we want to be Eclipse 3.4 compatible
	 *
	 * Applies decoration styles to the decorated string and adds the styles of the previously
	 * undecorated string.
	 * <p>
	 * If the <code>decoratedString</code> contains the <code>styledString</code>, then the result
	 * keeps the styles of the <code>styledString</code> and styles the decorations with the
	 * <code>decorationStyler</code>. Otherwise, the decorated string is returned without any
	 * styles.
	 *
	 * @param decoratedString
	 *            the decorated string
	 * @param decorationStyler
	 *            the styler to use for the decoration or <code>null</code> for no styles
	 * @param styledString
	 *            the original styled string
	 *
	 * @return the styled decorated string (can be the given <code>styledString</code>)
	 * @since 3.5
	 */
	private static StyledString styleDecoratedString(String decoratedString,
			Styler decorationStyler, StyledString styledString) {
		String label = styledString.getString();
		int originalStart = decoratedString.indexOf(label);
		if (originalStart == -1) {
			return new StyledString(decoratedString); // the decorator did something wild
		}

		if (decoratedString.length() == label.length()) {
			return styledString;
		}

		if (originalStart > 0) {
			StyledString newString = new StyledString(decoratedString.substring(0, originalStart),
					decorationStyler);
			newString.append(styledString);
			styledString = newString;
		}
		if (decoratedString.length() > originalStart + label.length()) { // decorator appended
			// something
			return styledString.append(decoratedString.substring(originalStart + label.length()),
					decorationStyler);
		}
		return styledString; // no change
	}

	private StyledString getMercurialMatchLabel(MercurialMatch match) {
		int lineNumber = match.getLineNumber();
		String becomesMatch = match.isBecomesMatch() ? "+" : "-";
		StyledString str = new StyledString(lineNumber + " (" + becomesMatch + ") ",
				StyledString.QUALIFIER_STYLER);

		String content = match.getExtract();

		return str.append(content);
	}

	private static final int MIN_MATCH_CONTEXT = 10; // minimal number of

	// characters shown
	// after and before a
	// match

	@Override
	public Image getImage(Object element) {
		if (element instanceof MercurialRevisionStorage) {
			return MercurialEclipsePlugin.getImage("elcl16/changeset_obj.gif");
		}

		if (element instanceof MercurialMatch) {
			return fLineMatchImage;
		}

		if (!(element instanceof IResource)) {
			return null;
		}

		return getResourceImage(element);
	}

	/**
	 * @param element
	 * @return
	 */
	protected Image getResourceImage(Object element) {
		IResource resource = (IResource) element;
		Image image = fLabelProvider.getImage(resource);
		return image;
	}

	@Override
	public void dispose() {
		super.dispose();
		fLabelProvider.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return fLabelProvider.isLabelProperty(element, property);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		fLabelProvider.removeListener(listener);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		fLabelProvider.addListener(listener);
	}

	/**
	 * @param mrs
	 * @return
	 */
	protected String getCsInfoString(MercurialRevisionStorage mrs) {
		ChangeSet cs = mrs.getChangeSet();
		String csInfo = cs.getChangesetIndex() + " [" + cs.getAuthor() + "] ("
				+ cs.getAgeDate() + ") ";
		return csInfo;
	}

}
