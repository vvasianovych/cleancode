/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.AbortRebaseHandler;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.menu.ContinueRebaseHandler;
import com.vectrace.MercurialEclipse.menu.RunnableHandler;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.CompareAction;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class MergeView extends ViewPart implements ISelectionListener, Observer {

	public static final String ID = MergeView.class.getName();

	private Label statusLabel;
	private Table table;

	private Action abortAction;

	private Action completeAction;

	private Action markResolvedAction;

	private Action markUnresolvedAction;

	protected HgRoot hgRoot;

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		statusLabel = new Label(parent, SWT.NONE);
		statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		table = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
				| SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 200;
		table.setLayoutData(data);

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				TableItem item = (TableItem) event.item;
				openMergeEditor(item);
			}
		});

		String[] titles = { Messages.getString("MergeView.column.status"), Messages.getString("MergeView.column.file") }; //$NON-NLS-1$ //$NON-NLS-2$
		int[] widths = { 100, 400 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}

		createToolBar();
		getSite().getPage().addSelectionListener(this);
		MercurialStatusCache.getInstance().addObserver(this);
	}

	private void createToolBar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		completeAction = new Action(Messages.getString("MergeView.complete")) { //$NON-NLS-1$
			@Override
			public void run() {
				if (areAllResolved()) {
					attemptToCommit();
					refresh(hgRoot);
				}
			}
		};
		completeAction.setEnabled(false);
		completeAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/commit.gif"));
		mgr.add(completeAction);

		abortAction = new Action(Messages.getString("MergeView.abort")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					RunnableHandler runnable;
					if (HgRebaseClient.isRebasing(hgRoot)) {
						runnable = new AbortRebaseHandler();
					} else {
						UpdateHandler update = new UpdateHandler();
						update.setCleanEnabled(true);
						update.setRevision(".");
						runnable = update;
					}

					runnable.setShell(table.getShell());
					runnable.run(hgRoot);
					refresh(hgRoot);
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
					statusLabel.setText(e.getLocalizedMessage());
				}
			}
		};
		abortAction.setEnabled(false);
		abortAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_ELCL_STOP));
		mgr.add(abortAction);
		markResolvedAction = new Action(Messages.getString("MergeView.markResolved")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					IFile file = getSelection();
					if (file != null) {
						HgResolveClient.markResolved(file);
						populateView(true);
					}
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
					statusLabel.setText(e.getLocalizedMessage());
				}
			}
		};
		markResolvedAction.setEnabled(false);
		markUnresolvedAction = new Action(Messages.getString("MergeView.markUnresolved")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					IFile file = getSelection();
					if (file != null) {
						HgResolveClient.markUnresolved(file);
						populateView(true);
					}
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
					statusLabel.setText(e.getLocalizedMessage());
				}
			}
		};
		markUnresolvedAction.setEnabled(false);

		final Action openMergeEditorAction = new Action("Open in Merge Editor") {
			@Override
			public void run() {
				TableItem[] selection = table.getSelection();
				if (selection != null && selection.length > 0) {
					openMergeEditor(selection[0]);
				}
			}
		};

		final Action openEditorAction = new Action("Open in Default Editor") {
			@Override
			public void run() {
				IFile file = getSelection();
				if(file == null){
					return;
				}
				ResourceUtils.openEditor(getSite().getPage(), file);
			}
		};

		final Action actionShowHistory = new Action("Show History") {
			@Override
			public void run() {
				IFile file = getSelection();
				if(file == null){
					return;
				}
				TeamUI.getHistoryView().showHistoryFor(file);
			}
		};
		actionShowHistory.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("history.gif"));

		// Contribute actions to popup menu
		final MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(table);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr1) {
				menuMgr1.add(openMergeEditorAction);
				menuMgr1.add(openEditorAction);
				menuMgr1.add(new Separator());
				menuMgr1.add(actionShowHistory);
				menuMgr1.add(new Separator());
				menuMgr1.add(markResolvedAction);
				menuMgr1.add(markUnresolvedAction);
				menuMgr1.add(new Separator());
				menuMgr1.add(completeAction);
				menuMgr1.add(abortAction);
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		table.setMenu(menu);
	}

	private void populateView(boolean attemptToCommit) throws HgException {
		boolean bAllResolved = true;
		List<FlaggedAdaptable> status = null;
		status = HgResolveClient.list(hgRoot);
		table.removeAll();
		for (FlaggedAdaptable flagged : status) {
			TableItem row = new TableItem(table, SWT.NONE);
			row.setText(0, flagged.getStatus());
			IFile iFile = (IFile) flagged.getAdapter(IFile.class);
			row.setText(1, iFile.getProjectRelativePath().toString());
			row.setData(flagged);
			if (flagged.getFlag() == MercurialStatusCache.CHAR_UNRESOLVED) {
				row.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
				bAllResolved = false;
			}
		}
		abortAction.setEnabled(true);
		completeAction.setEnabled(true);
		markResolvedAction.setEnabled(true);
		markUnresolvedAction.setEnabled(true);

		// Update the status label
		{
			boolean merging = !HgRebaseClient.isRebasing(hgRoot);
			String label;

			if (bAllResolved) {
				if (merging) {
					label = hgRoot.getName() + Messages.getString("MergeView.PleaseCommitMerge")
							+ " " + MercurialStatusCache.getInstance().getMergeChangesetId(hgRoot);
				} else {
					label = hgRoot.getName() + Messages.getString("MergeView.PleaseCommitRebase");
				}
			} else {
				if (merging) {
					String mergeNodeId = MercurialStatusCache.getInstance().getMergeChangesetId(
							hgRoot);
					if (mergeNodeId != null) {
						label = "Merging " + hgRoot.getName() + " with " + mergeNodeId;
					} else {
						label = "Merging " + hgRoot.getName();
					}
				} else {
					label = "Rebasing";
				}
			}

			statusLabel.setText(label);
		}

		// Show commit dialog
		if (attemptToCommit && !MercurialUtilities.isMergeViewDialogShown()
				&& areAllResolved()) {
			/*
			 * Offer commit of merge or rebase exactly once if no conflicts are found. Uses {@link
			 * ResourceProperties#MERGE_COMMIT_OFFERED} to avoid showing the user the commit dialog
			 * repeatedly. This flag should be cleared when any of the following operations occur:
			 * commit, rebase, revert.
			 */
			MercurialUtilities.setMergeViewDialogShown(true);
			attemptToCommit();
		}
	}

	private void attemptToCommit() {
		try {
			boolean merging = !HgRebaseClient.isRebasing(hgRoot);

			MercurialUtilities.setMergeViewDialogShown(true);
			RunnableHandler handler = merging ? new CommitMergeHandler()
					: new ContinueRebaseHandler();

			handler.setShell(getSite().getShell());
			handler.run(hgRoot);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	protected void clearView() {
		statusLabel.setText("");
		table.removeAll();
		abortAction.setEnabled(false);
		completeAction.setEnabled(false);
		markResolvedAction.setEnabled(false);
		markUnresolvedAction.setEnabled(false);
		hgRoot = null;
	}

	public void refresh(HgRoot newRoot) {
		clearView();
		setCurrentRoot(newRoot);
	}

	/**
	 * Refresh the view with for the given hg root.
	 *
	 * @param newRoot The new selection
	 */
	protected void setCurrentRoot(HgRoot newRoot) {
		if(newRoot == null) {
			clearView();
			return;
		}
		if ((hgRoot == null) || !newRoot.equals(hgRoot)) {
			// TODO should schedule a job here...
			try {
				if (MercurialStatusCache.getInstance().isMergeInProgress(newRoot)) {
					this.hgRoot = newRoot;
					populateView(false);
				} else {
					clearView();
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	private boolean areAllResolved() {
		boolean allResolved = true;
		if (table.getItems() != null && table.getItems().length > 0) {
			for (TableItem item : table.getItems()) {
				FlaggedAdaptable fa = (FlaggedAdaptable) item.getData();
				allResolved &= fa.getFlag() == MercurialStatusCache.CHAR_RESOLVED;
			}
		}
		return allResolved;
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// TODO do not react on any changes if the view is hidden...

		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			IResource resource = MercurialEclipsePlugin.getAdapter(structured.getFirstElement(), IResource.class);
			if (resource != null) {
				setCurrentRoot(MercurialTeamProvider.hasHgRoot(resource));
			}
		}
	}

	@Override
	public void setFocus() {
		table.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		MercurialStatusCache.getInstance().deleteObserver(this);
		super.dispose();
	}

	private IFile getSelection() {
		TableItem[] selection = table.getSelection();
		if (selection != null && selection.length > 0) {
			FlaggedAdaptable fa = (FlaggedAdaptable) selection[0].getData();
			IFile iFile = (IFile) fa.getAdapter(IFile.class);
			return iFile;
		}
		return null;
	}

	public void update(Observable o, Object arg) {
		if(hgRoot == null || !(arg instanceof Set<?>)){
			return;
		}
		Set<?> set = (Set<?>) arg;
		Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
		// create intersection of the root projects with the updated set
		projects.retainAll(set);
		// if the intersection contains common projects, we need update the view
		if(!projects.isEmpty()) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					refresh(hgRoot);
				}
			});
		}
	}

	private void openMergeEditor(TableItem item) {
		FlaggedAdaptable flagged = (FlaggedAdaptable) item.getData();
		IFile file = (IFile) flagged.getAdapter(IFile.class);
		CompareAction compareAction = new CompareAction(file);
		compareAction.setEnableMerge(true);
		compareAction.run(null);
	}

}
