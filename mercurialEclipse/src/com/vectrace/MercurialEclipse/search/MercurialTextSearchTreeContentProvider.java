/*******************************************************************************
 * Copyright (c) 2000, 2008, 2010 IBM Corporation and others.
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
 *     Bastian Doetsch - Adaptation for MercurialEclipse
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

public class MercurialTextSearchTreeContentProvider implements ITreeContentProvider,
		IMercurialTextSearchContentProvider {

	private static final Object[] EMPTY_ARR = new Object[0];

	private AbstractTextSearchResult fResult;
	private final MercurialTextSearchResultPage fPage;
	private final AbstractTreeViewer fTreeViewer;
	private Map fChildrenMap;

	MercurialTextSearchTreeContentProvider(MercurialTextSearchResultPage page,
			AbstractTreeViewer viewer) {
		fPage = page;
		fTreeViewer = viewer;
	}

	public Object[] getElements(Object inputElement) {
		Object[] children = getChildren(inputElement);
		int elementLimit = getElementLimit();
		if (elementLimit != -1 && elementLimit < children.length) {
			Object[] limitedChildren = new Object[elementLimit];
			System.arraycopy(children, 0, limitedChildren, 0, elementLimit);
			return limitedChildren;
		}
		return children;
	}

	private int getElementLimit() {
		return fPage.getElementLimit().intValue();
	}

	public void dispose() {
		// nothing to do
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof MercurialTextSearchResult) {
			initialize((MercurialTextSearchResult) newInput);
		}
	}

	private synchronized void initialize(AbstractTextSearchResult result) {
		fResult = result;
		fChildrenMap = new HashMap();
		addMatches(result);
	}

	/**
	 * @param result
	 * @param showLineMatches
	 */
	private void addMatches(AbstractTextSearchResult result) {
		boolean showLineMatches = !((MercurialTextSearchQuery) fResult.getQuery())
				.isFileNameSearch();

		if (result != null && showLineMatches) {
			Object[] elements = result.getElements();
			for (int i = 0; i < elements.length; i++) {
				Match[] matches = result.getMatches(elements[i]);
				for (int j = 0; j < matches.length; j++) {
					MercurialMatch m = (MercurialMatch) matches[j];
					insert(m, false, m.getMercurialRevisionStorage());
				}
			}
		}
	}

	private void insert(Object child, boolean refreshViewer, MercurialRevisionStorage mrs) {
		Object parent = getParent(child, mrs);
		while (parent != null) {
			if (insertChild(parent, child)) {
				if (refreshViewer) {
					fTreeViewer.add(parent, child);
				}
			} else {
				if (refreshViewer) {
					fTreeViewer.refresh(parent);
				}
				return;
			}
			child = parent;
			parent = getParent(child, mrs);
		}
		if (insertChild(fResult, child)) {
			if (refreshViewer) {
				fTreeViewer.add(fResult, child);
			}
		}
	}

	/**
	 * Adds the child to the parent.
	 *
	 * @param parent
	 *            the parent
	 * @param child
	 *            the child
	 * @return <code>true</code> if this set did not already contain the specified element
	 */
	private boolean insertChild(Object parent, Object child) {
		Set<Object> children = (Set<Object>) fChildrenMap.get(parent);
		if (children == null) {
			children = new HashSet();
			fChildrenMap.put(parent, children);
		}
		return children.add(child);
	}

	private boolean hasChild(Object parent, Object child) {
		Set children = (Set) fChildrenMap.get(parent);
		return children != null && children.contains(child);
	}

	private void remove(Object element, boolean refreshViewer) {
		// precondition here: fResult.getMatchCount(child) <= 0

		if (hasChildren(element)) {
			if (refreshViewer) {
				fTreeViewer.refresh(element);
			}
		} else {
			if (!hasMatches(element)) {
				fChildrenMap.remove(element);
				Object parent = getParent(element);
				if (parent != null) {
					removeFromSiblings(element, parent);
					remove(parent, refreshViewer);
				} else {
					removeFromSiblings(element, fResult);
					if (refreshViewer) {
						fTreeViewer.refresh();
					}
				}
			} else {
				if (refreshViewer) {
					fTreeViewer.refresh(element);
				}
			}
		}
	}

	private boolean hasMatches(Object element) {
		return fResult.getMatchCount(element) > 0;
	}

	private void removeFromSiblings(Object element, Object parent) {
		Set siblings = (Set) fChildrenMap.get(parent);
		if (siblings != null) {
			siblings.remove(element);
		}
	}

	public Object[] getChildren(Object parentElement) {
		Set children = (Set) fChildrenMap.get(parentElement);
		if (children == null) {
			return EMPTY_ARR;
		}
		return children.toArray();
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.eclipse.search.internal.ui.text.IFileSearchContentProvider#
	 * elementsChanged(java.lang.Object[])
	 */
	public synchronized void elementsChanged(Object[] updatedElements) {
		for (int i = 0; i < updatedElements.length; i++) {
			if (!(updatedElements[i] instanceof MercurialRevisionStorage)) {
				// do nothing
			} else {
				MercurialRevisionStorage mrs = (MercurialRevisionStorage) updatedElements[i];
				insert(mrs.getResource(), true, mrs);
				addMatches(fResult);
			}
		}
	}

	public void clear() {
		initialize(fResult);
		fTreeViewer.refresh();
	}

	public Object getParent(Object element, MercurialRevisionStorage mrs) {
		if (element instanceof IProject) {
			return null;
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			return resource.getParent();
		}

		if (element instanceof MercurialRevisionStorage) {
			return mrs.getResource();
		}

		if (element instanceof MercurialMatch) {
			return mrs;
		}
		return null;
	}

	public Object getParent(Object element) {
		return null;
	}
}
