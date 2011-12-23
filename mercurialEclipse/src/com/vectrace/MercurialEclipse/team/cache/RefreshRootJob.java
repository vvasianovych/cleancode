/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - init
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistoryPage;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * Refreshes working directory status, local changesets, incoming changesets and outgoing
 * changesets. If you only want to refresh the working directory status use {@link RefreshStatusJob}.
 * If you want additionally refresh Eclipse workspace data, use {@link RefreshWorkspaceStatusJob}.
 *
 * <p>
 * This job uses exclusive rule based on given hg root (see {@link HgRootRule}). It means, this job
 * never runs in parallel with any other job using the same rule on given hg root.
 *
 * @author Andrei Loskutov
 * @author Bastian Doetsch
 */
public class RefreshRootJob extends Job {

	public static final int LOCAL = 1 << 0;
	public static final int INCOMING = 1 << 1;
	public static final int OUTGOING = 1 << 2;
	/** right now only used by {@link RefreshWorkspaceStatusJob} */
	public static final int WORKSPACE = 1 << 3;

	public static final int LOCAL_AND_INCOMING = LOCAL | INCOMING;
	public static final int LOCAL_AND_OUTGOING = LOCAL | OUTGOING;

	/** refresh local, incoming and outgoing cache. Does not include workspace refresh */
	public static final int ALL = LOCAL | INCOMING | OUTGOING;

	protected final HgRoot hgRoot;
	private final int type;

	/**
	 * If the flags are non zero, refreshes different mercurial caches for the given root.
	 * @param hgRoot non null
	 * @param flags one of RefreshRootJob flags
	 */
	public RefreshRootJob(HgRoot hgRoot, int flags) {
		this("Refreshing " + hgRoot.getName(), hgRoot, flags);
	}

	/**
	 * If the flags are non zero, refreshes different mercurial caches for the given root.
	 * @param name non null job name, shown to user
	 * @param hgRoot non null
	 * @param flags one of RefreshRootJob flags
	 */
	public RefreshRootJob(String name, HgRoot hgRoot, int flags) {
		super(name);
		Assert.isNotNull(hgRoot);
		this.hgRoot = hgRoot;
		this.type = flags;
		setRule(new HgRootRule(hgRoot));
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if((type & LOCAL) != 0){
			if(monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			trace("LOCAL");
			monitor.subTask(Messages.refreshJob_LoadingLocalRevisions);
			LocalChangesetCache.getInstance().clear(hgRoot, true);
			monitor.worked(1);

			monitor.subTask(Messages.refreshJob_UpdatingStatusAndVersionCache);
			MercurialStatusCache statusCache = MercurialStatusCache.getInstance();
			statusCache.clear(hgRoot, false);
			try {
				statusCache.refreshStatus(hgRoot, monitor);
				monitor.worked(1);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
			updateHistoryView();
		}

		if((type & INCOMING) != 0){
			if(monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			trace("INCOMING");
			monitor.subTask(NLS.bind(Messages.refreshJob_LoadingIncomingRevisions, hgRoot.getName()));
			IncomingChangesetCache.getInstance().clear(hgRoot, true);
			monitor.worked(1);
		}

		if((type & OUTGOING) != 0){
			if(monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			trace("OUTGOING");
			monitor.subTask(NLS.bind(Messages.refreshJob_LoadingOutgoingRevisionsFor, hgRoot.getName()));
			OutgoingChangesetCache.getInstance().clear(hgRoot, true);
			monitor.worked(1);
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	/**
	 * Running in UI thread asynchronously
	 */
	private void updateHistoryView() {
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
				for (IWorkbenchWindow ww : windows) {
					IViewPart view = ww.getActivePage().findView(IHistoryView.VIEW_ID);
					if(!(view instanceof IHistoryView)) {
						continue;
					}
					IHistoryView hview = (IHistoryView) view;
					IHistoryPage page = hview.getHistoryPage();
					if(!(page instanceof MercurialHistoryPage)) {
						continue;
					}
					MercurialHistoryPage mhp = (MercurialHistoryPage) page;
					mhp.refresh();
				}
			}
		});
	}

	/**
	 * @param step current job task, non null
	 */
	protected void trace(String step) {
		if(MercurialEclipsePlugin.getDefault().isDebugging()) {
			System.out.println("Refresh " + step + " for: " + hgRoot.getName());
		}
	}

	@Override
	public boolean belongsTo(Object family) {
		return RefreshRootJob.class == family;
	}

	@Override
	public boolean shouldSchedule() {
		Job[] jobs = Job.getJobManager().find(RefreshRootJob.class);
		for (Job job : jobs) {
			if(isSimilar(job)){
				// do not schedule me because exactly the same job is waiting to be started!
				return false;
			}
		}
		return true;
	}

	protected boolean isSimilar(Job job) {
		if(!(job instanceof RefreshRootJob)){
			return false;
		}
		RefreshRootJob rootJob = (RefreshRootJob) job;
		int state = rootJob.getState();
		return type == rootJob.type && (state == WAITING || state == SLEEPING)
				&& hgRoot.equals(rootJob.hgRoot);
	}
}