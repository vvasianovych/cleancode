/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 *     Bastian Doetsch - adaptation for MercurialEclipse
 *     Philip Graf - Fixed bugs which FindBugs found
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.text.NewTextSearchActionGroup;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search2.internal.ui.OpenSearchPreferencesAction;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.ITextEditor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

@SuppressWarnings("restriction")
public class MercurialTextSearchResultPage extends AbstractTextSearchViewPage implements IAdaptable {

	private static final String KEY_LIMIT = "org.eclipse.search.resultpage.limit"; //$NON-NLS-1$

	private static final int DEFAULT_ELEMENT_LIMIT = 1000;

	private ActionGroup fActionGroup;
	private IMercurialTextSearchContentProvider contentProvider;

	public MercurialTextSearchResultPage() {
		setElementLimit(new Integer(DEFAULT_ELEMENT_LIMIT));
	}

	@Override
	public void setElementLimit(Integer elementLimit) {
		super.setElementLimit(elementLimit);
		int limit = elementLimit.intValue();
		getSettings().put(KEY_LIMIT, limit);
	}

	@Override
	public StructuredViewer getViewer() {
		return super.getViewer();
	}

	@Override
	protected void configureTableViewer(TableViewer viewer) {
		viewer.setUseHashlookup(true);
		contentProvider = new MercurialTextSearchTableContentProvider(this);
		viewer.setContentProvider(contentProvider);
		MercurialTextSearchTableLabelProvider innerLabelProvider = new MercurialTextSearchTableLabelProvider(
				this, MercurialTextSearchTreeLabelProvider.SHOW_LABEL);
		viewer.setLabelProvider(new DecoratingMercurialTextSearchLabelProvider(innerLabelProvider));
	}

	@Override
	protected void configureTreeViewer(TreeViewer viewer) {
		viewer.setUseHashlookup(true);
		contentProvider = new MercurialTextSearchTreeContentProvider(this, viewer);
		viewer.setContentProvider(contentProvider);
		MercurialTextSearchTreeLabelProvider innerLabelProvider = new MercurialTextSearchTreeLabelProvider(
				this, MercurialTextSearchTreeLabelProvider.SHOW_LABEL);
		viewer.setLabelProvider(new DecoratingMercurialTextSearchLabelProvider(innerLabelProvider));
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer v, Object e1, Object e2) {
				if (e1 instanceof MercurialRevisionStorage
						&& e2 instanceof MercurialRevisionStorage) {
					MercurialRevisionStorage mrs1 = (MercurialRevisionStorage) e1;
					MercurialRevisionStorage mrs2 = (MercurialRevisionStorage) e2;
					if (mrs1.getResource().equals(mrs2.getResource())) {
						return mrs2.getRevision() - mrs1.getRevision();
					}
				} else if (e1 instanceof MercurialMatch && e2 instanceof MercurialMatch) {
					MercurialMatch m1 = (MercurialMatch) e1;
					MercurialMatch m2 = (MercurialMatch) e2;
					return m1.getLineNumber() - m2.getLineNumber();
				}
				return super.compare(v, e1, e2);
			}
		});
	}

	@Override
	protected void showMatch(Match match, int offset, int length, boolean activate)
			throws PartInitException {
		// TODO
	}

	@Override
	protected void handleOpen(OpenEvent event) {
			Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
			if (firstElement instanceof MercurialMatch) {
				if (getDisplayedMatchCount(firstElement) == 0) {
					try {
						MercurialMatch m = (MercurialMatch) firstElement;
						// open an editor with the content of the changeset this matc
						MercurialRevision revision = new MercurialRevision(m
								.getMercurialRevisionStorage().getChangeSet(), null, m.getFile(),
								null, null);
						IEditorPart editor = Utils.openEditor(getSite().getPage(), revision,
								new NullProgressMonitor());
						if (editor instanceof ITextEditor) {
							ITextEditor textEditor = (ITextEditor) editor;
							IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
							if (document != null) {
								String content = document.get();
								int offset = content.indexOf(m.getExtract());
								int length = m.getExtract().length();
								m.setOffset(offset);
								m.setLength(length);
								textEditor.selectAndReveal(offset, length);
							}
						}
					} catch (Exception e) {
						ErrorDialog.openError(getSite().getShell(),
								SearchMessages.FileSearchPage_open_file_dialog_title,
								SearchMessages.FileSearchPage_open_file_failed, new Status(
										IStatus.ERROR, MercurialEclipsePlugin.ID, e
												.getLocalizedMessage(), e));
					}
					return;
				}
		}
	}

	@Override
	protected void fillContextMenu(IMenuManager mgr) {
		// TODO
	}

	@Override
	public void setViewPart(ISearchResultViewPart part) {
		super.setViewPart(part);
		fActionGroup = new NewTextSearchActionGroup(part);
	}

	@Override
	public void init(IPageSite site) {
		super.init(site);
		IMenuManager menuManager = site.getActionBars().getMenuManager();
		menuManager.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES,
				new OpenSearchPreferencesAction());
	}

	@Override
	public void dispose() {
		fActionGroup.dispose();
		super.dispose();
	}

	@Override
	protected void elementsChanged(Object[] objects) {
		if (contentProvider != null) {
			contentProvider.elementsChanged(objects);
		}
	}

	@Override
	protected void clear() {
		if (contentProvider != null) {
			contentProvider.clear();
		}
	}

	@Override
	public void restoreState(IMemento memento) {
		super.restoreState(memento);
		int elementLimit = DEFAULT_ELEMENT_LIMIT;
		try {
			elementLimit = getSettings().getInt(KEY_LIMIT);
		} catch (NumberFormatException e) {
		}
		if (memento != null) {
			Integer value = memento.getInteger(KEY_LIMIT);
			if (value != null) {
				elementLimit = value.intValue();
			}
		}
		setElementLimit(Integer.valueOf(elementLimit));
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putInteger(KEY_LIMIT, getElementLimit().intValue());
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public String getLabel() {
		String label = super.getLabel();
		StructuredViewer viewer = getViewer();
		if (viewer instanceof TableViewer) {
			TableViewer tv = (TableViewer) viewer;

			AbstractTextSearchResult result = getInput();
			if (result != null) {
				int itemCount = ((IStructuredContentProvider) tv.getContentProvider())
						.getElements(getInput()).length;
				if (showLineMatches()) {
					int matchCount = getInput().getMatchCount();
					if (itemCount < matchCount) {
						return Messages.format(
								SearchMessages.FileSearchPage_limited_format_matches,
								new Object[] {label, Integer.valueOf(itemCount), Integer.valueOf(matchCount)});
					}
				} else {
					int fileCount = getInput().getElements().length;
					if (itemCount < fileCount) {
						return Messages.format(
								SearchMessages.FileSearchPage_limited_format_files,
								new Object[] {label, Integer.valueOf(itemCount), Integer.valueOf(fileCount)});
					}
				}
			}
		}
		return label;
	}

	private boolean showLineMatches() {
		AbstractTextSearchResult input = getInput();
		return getLayout() == FLAG_LAYOUT_TREE && input != null
				&& !((MercurialTextSearchQuery) input.getQuery()).isFileNameSearch();
	}

}
