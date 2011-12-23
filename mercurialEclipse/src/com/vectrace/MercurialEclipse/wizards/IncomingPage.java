/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei Loskutov - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.SimpleLabelImageProvider;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.ChangeSet.ParentChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.NullRevision;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangeSetLabelProvider;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class IncomingPage extends HgWizardPage {

	private TableViewer changeSetViewer;
	private TableViewer fileStatusViewer;
	private HgRoot hgRoot;
	private IHgRepositoryLocation location;
	protected Button revisionCheckBox;
	private ChangeSet revision;
	private SortedSet<ChangeSet> changesets;
	private boolean svn;
	private boolean force;

	private class GetIncomingOperation extends HgOperation {

		public GetIncomingOperation(IRunnableContext context) {
			super(context);
		}

		@Override
		protected String getActionDescription() {
			return Messages.getString("IncomingPage.getIncomingOperation.description"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("IncomingPage.getIncomingOperation.beginTask"), 1); //$NON-NLS-1$
			monitor.subTask(Messages.getString("IncomingPage.getIncomingOperation.call")); //$NON-NLS-1$
			changesets = getIncomingInternal();
			monitor.worked(1);
			monitor.done();
		}

		private SortedSet<ChangeSet> getIncomingInternal() {
			if (isSvn()) {
				return new TreeSet<ChangeSet>();
			}
			IncomingChangesetCache cache = IncomingChangesetCache.getInstance();
			try {
				cache.clear(hgRoot, false);
				Set<ChangeSet> set = cache.getChangeSets(hgRoot, location, null, isForce());
				SortedSet<ChangeSet> revertedSet = new TreeSet<ChangeSet>(Collections.reverseOrder());
				revertedSet.addAll(set);
				return revertedSet;
			} catch (HgException e) {
				MercurialEclipsePlugin.showError(e);
				return new TreeSet<ChangeSet>();
			}
		}
	}

	protected class IncomingDoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent event) {
			ChangeSet cs = getSelectedChangeSet();
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			FileStatus clickedFileStatus = (FileStatus) sel.getFirstElement();

			if (cs != null && clickedFileStatus != null) {
				IPath fileAbsPath = hgRoot.toAbsolute(clickedFileStatus.getRootRelativePath());
				IFile file = ResourceUtils.getFileHandle(fileAbsPath);

				if (file != null) {
					MercurialRevisionStorage remoteRev = new MercurialRevisionStorage(
							file, cs.getChangesetIndex(), cs.getChangeset(), cs);

					MercurialRevisionStorage parentRev;
					String[] parents = cs.getParents();
					if(cs.getRevision().getRevision() == 0 || parents.length == 0){
						parentRev = new NullRevision(file, cs);
					} else {
						String parentId = parents[0];
						ChangeSet parentCs = null;
						for (ChangeSet cset : changesets) {
							if(parentId.endsWith(cset.getChangeset())
									&& parentId.startsWith("" + cset.getChangesetIndex())){
								parentCs = cset;
								break;
							}
						}
						if(parentCs == null) {
							parentCs = new ParentChangeSet(parentId, cs);
						}
						if(clickedFileStatus.isCopied()){
							IPath fileCopySrcPath = hgRoot.toAbsolute(clickedFileStatus.getRootRelativeCopySourcePath());
							IFile copySrc = ResourceUtils.getFileHandle(fileCopySrcPath);
							parentRev = new MercurialRevisionStorage(
									copySrc, parentCs.getChangesetIndex(), parentCs.getChangeset(), parentCs);

						}else{
							parentRev = new MercurialRevisionStorage(
									file, parentCs.getChangesetIndex(), parentCs.getChangeset(), parentCs);
						}
					}
					CompareUtils.openEditor(remoteRev, parentRev, true);
					// the line below compares the remote changeset with the local copy.
					// it was replaced with the code above to fix the issue 10364
					// CompareUtils.openEditor(file, cs, true, true);
				} else {
					// It is possible that file has been removed or part of the
					// repository but not the project (and has incoming changes)
					MercurialEclipsePlugin.showError(new FileNotFoundException(Messages.getString("IncomingPage.compare.file.missing")));
				}
			}
		}
	}

	protected IncomingPage(String pageName) {
		super(pageName);
		setTitle(Messages.getString("IncomingPage.title")); //$NON-NLS-1$
		setDescription(Messages.getString("IncomingPage.description")); //$NON-NLS-1$
	}

	public IHgRepositoryLocation getLocation() {
		return location;
	}

	public void setRevision(ChangeSet revision) {
		this.revision = revision;
	}

	public void setChangesets(SortedSet<ChangeSet> changesets) {
		this.changesets = changesets;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			try {
				getInputForPage();
				changeSetViewer.setInput(changesets);
			} catch (InvocationTargetException e) {
				MercurialEclipsePlugin.logError(e);
				setErrorMessage(e.getLocalizedMessage());
			} catch (InterruptedException e) {
				MercurialEclipsePlugin.logError(e);
				setErrorMessage(e.getLocalizedMessage());
			}
		}
	}

	protected void getInputForPage() throws InvocationTargetException,
			InterruptedException {
		getContainer().run(true, false,
				new GetIncomingOperation(getContainer()));
	}

	public void createControl(Composite parent) {

		Composite container = SWTWidgetHelper.createComposite(parent, 1);
		setControl(container);

		changeSetViewer = new TableViewer(container, SWT.SINGLE | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		changeSetViewer.setContentProvider(new ArrayContentProvider());
		changeSetViewer.setLabelProvider(new ChangeSetLabelProvider());
		Table table = changeSetViewer.getTable();
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		table.setLayoutData(gridData);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		String[] titles = {
				Messages.getString("IncomingPage.columnHeader.revision"), //$NON-NLS-1$
				Messages.getString("IncomingPage.columnHeader.global"), //$NON-NLS-1$
				Messages.getString("IncomingPage.columnHeader.date"), //$NON-NLS-1$
				Messages.getString("IncomingPage.columnHeader.author"), //$NON-NLS-1$
				Messages.getString("IncomingPage.columnHeader.branch"), //$NON-NLS-1$
				"Tags", //$NON-NLS-1$
				Messages.getString("IncomingPage.columnHeader.summary") };  //$NON-NLS-1$
		final int width = 11;
		int[] widths = {6 * width, 7 * width, 15 * width, 14 * width, 5 * width, 5 * width, 30 * width};
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}

		fileStatusViewer = new TableViewer(container, SWT.SINGLE | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		fileStatusViewer.setContentProvider(new ArrayContentProvider());
		fileStatusViewer.setLabelProvider(new FileStatusLabelProvider());

		table = fileStatusViewer.getTable();
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		table.setLayoutData(gridData);
		table.setHeaderVisible(false);
		table.setLinesVisible(true);

		Group group = SWTWidgetHelper.createGroup(container, Messages
				.getString("IncomingPage.group.title")); //$NON-NLS-1$
		revisionCheckBox = SWTWidgetHelper.createCheckBox(group, Messages
				.getString("IncomingPage.revisionCheckBox.title")); //$NON-NLS-1$
		makeActions();
	}

	ChangeSet getSelectedChangeSet() {
		IStructuredSelection sel = (IStructuredSelection) changeSetViewer
				.getSelection();
		Object firstElement = sel.getFirstElement();
		if (firstElement instanceof ChangeSet) {
			return (ChangeSet) firstElement;
		}
		return null;
	}

	private void makeActions() {
		changeSetViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						ChangeSet change = getSelectedChangeSet();
						revision = change;
						if (change != null) {
							fileStatusViewer.setInput(change.getChangedFiles());
						} else {
							fileStatusViewer.setInput(new Object[0]);
						}
					}
				});

		fileStatusViewer.addDoubleClickListener(getDoubleClickListener());
	}

	private static final class FileStatusLabelProvider extends DecoratingLabelProvider implements
		ITableLabelProvider  {

		public FileStatusLabelProvider() {
			super(new SimpleLabelImageProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			return getImage(element);
		}

		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			return getText(element);
		}
	}

	public void setHgRoot(HgRoot hgRoot) {
		this.hgRoot = hgRoot;
	}

	public HgRoot getHgRoot() {
		return hgRoot;
	}

	public void setLocation(IHgRepositoryLocation repo) {
		this.location = repo;
	}

	public boolean isRevisionSelected() {
		return revisionCheckBox.getSelection();
	}

	public ChangeSet getRevision() {
		return revision;
	}

	public SortedSet<ChangeSet> getChangesets() {
		return changesets;
	}

	public void setSvn(boolean svn) {
		this.svn = svn;
	}

	public boolean isSvn() {
		return svn;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isForce() {
		return force;
	}

	protected IDoubleClickListener getDoubleClickListener() {
		return new IncomingDoubleClickListener();
	}
}
