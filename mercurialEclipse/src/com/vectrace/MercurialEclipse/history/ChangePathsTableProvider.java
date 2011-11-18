/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Andrei Loskutov 		 - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class ChangePathsTableProvider extends TableViewer {

	private static final FileStatus[] EMPTY_CHANGE_PATHS = new FileStatus[0];
	private final ChangedPathsPage page;
	private final ChangePathsTableContentProvider contentProvider;

	public ChangePathsTableProvider(Composite parent, ChangedPathsPage page) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		this.page = page;
		contentProvider = new ChangePathsTableContentProvider(page);
		setContentProvider(contentProvider);

		setLabelProvider(new ChangePathLabelProvider(page, this));

		GridData data = new GridData(GridData.FILL_BOTH);

		final Table table = (Table) getControl();
		table.setHeaderVisible(false);
		table.setLinesVisible(true);
		table.setLayoutData(data);
	}

	public int getElementsCount(){
		MercurialRevision revision = page.getCurrentRevision();
		if(revision == null || !revision.isFile()){
			return 0;
		}
		return contentProvider.getElements(revision).length;
	}

	private static class ChangePathLabelProvider extends DecoratingLabelProvider implements
			ITableLabelProvider {

		private final ChangedPathsPage page;
		private final ChangePathsTableProvider tableProvider;

		public ChangePathLabelProvider(ChangedPathsPage page, ChangePathsTableProvider tableProvider) {
			super(new SimpleLabelImageProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
			this.page = page;
			this.tableProvider = tableProvider;
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

		@Override
		public Font getFont(Object element) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			MercurialRevision revision = page.getCurrentRevision();
			if(revision == null || !revision.isFile()){
				return null;
			}
			IPath basePath = ResourceUtils.getPath(revision.getResource());
			IPath currentPath = ((FileStatus) element).getAbsolutePath();
			if(basePath.equals(currentPath) && tableProvider.getElementsCount() > 1) {
				// highlight current file in the changeset, if there are more files
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
			}
			return JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
		}

	}

	private static class ChangePathsTableContentProvider implements
			IStructuredContentProvider {

		private final WeakHashMap<MercurialRevision, FileStatus[]> revToFiles;
		private final ChangedPathsPage page;
		private Viewer viewer;
		private boolean disposed;

		public ChangePathsTableContentProvider(ChangedPathsPage page) {
			this.page = page;
			revToFiles = new WeakHashMap<MercurialRevision, FileStatus[]>();
		}

		public Object[] getElements(Object inputElement) {
			if (!this.page.isShowChangePaths()) {
				return EMPTY_CHANGE_PATHS;
			}

			MercurialRevision rev = (MercurialRevision) inputElement;
			FileStatus[] fileStatus;
			synchronized(revToFiles){
				fileStatus = revToFiles.get(rev);
			}
			if(fileStatus != null){
				return fileStatus;
			}
			fetchPaths(rev);
			// but sometimes hg returns a null version map...
			return EMPTY_CHANGE_PATHS;
		}

		private void fetchPaths(final MercurialRevision rev) {
			final MercurialHistory history = page.getMercurialHistory();
			final ChangeSet [] cs = new ChangeSet[1];
			Job.getJobManager().cancel(ChangePathsTableContentProvider.class);
			Job pathJob = new Job(NLS.bind(
					Messages.ChangePathsTableProvider_retrievingAffectedPaths, rev.getChangeSet())) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					synchronized(revToFiles){
						if(revToFiles.get(rev) != null || monitor.isCanceled()){
							return Status.CANCEL_STATUS;
						}
					}
					try {
						cs[0] = HgLogClient.getLogWithBranchInfo(rev, history, monitor);
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
						return e.getStatus();
					}
					return monitor.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return ChangePathsTableContentProvider.class == family;
				}
			};
			pathJob.setRule(new ExclusiveHistoryRule());
			pathJob.addJobChangeListener(new JobChangeAdapter(){
				@Override
				public void done(IJobChangeEvent event) {
					if(!event.getResult().isOK()){
						return;
					}
					FileStatus[] changedFiles = EMPTY_CHANGE_PATHS;
					ChangeSet fullCs = cs[0];
					if(fullCs != null) {
						ChangeSet revCs = rev.getChangeSet();
						// TODO this is a workaround: we copy some info from freshly retrieved cs
						// because this data is NOT currently available in the history view changesets (different template)
						// but it is nice to show it in the properties view
						revCs.setTags(fullCs.getTags());
						revCs.setTagsStr(fullCs.getTagsStr());
						List<FileStatus> list = fullCs.getChangedFiles();
						revCs.setChangedFiles(list);
						changedFiles = list.toArray(new FileStatus[list.size()]);
						if(changedFiles.length == 0){
							changedFiles = EMPTY_CHANGE_PATHS;
						}
					}
					synchronized(revToFiles){
						if(!revToFiles.containsKey(rev)) {
							revToFiles.put(rev, changedFiles);
						}
					}
					if(disposed){
						return;
					}
					Runnable refresh = new Runnable() {
						public void run() {
							if(!disposed && viewer != null) {
								viewer.refresh();
							}
						}
					};
					Display.getDefault().asyncExec(refresh);
				}
			});
			if(!disposed) {
				page.getHistoryPage().scheduleInPage(pathJob);
			}
		}

		public void dispose() {
			disposed = true;
			synchronized(revToFiles){
				revToFiles.clear();
			}
		}

		public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
			this.viewer = viewer1;
		}
	}
}
