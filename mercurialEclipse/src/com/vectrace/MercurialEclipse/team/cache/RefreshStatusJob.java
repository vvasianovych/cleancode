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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 *
 */
public class RefreshStatusJob extends Job {

	private static final MercurialStatusCache MERCURIAL_STATUS_CACHE = MercurialStatusCache
			.getInstance();
	private final IProject project;
	private final HgRoot hgRoot;

	/**
	 * true to re-check the project state before starting the job and if the project
	 * state is known, cancel the job
	 */
	private boolean updateRootIfProjectUnknown;

	/**
	 * Special constructor for job which ONLY then will be started, if the status of given
	 * project is unknown at the time the jobs starts.
	 * @param name non null
	 * @param project non null
	 * @param root non null
	 */
	public RefreshStatusJob(String name, IProject project, HgRoot root) {
		super(name);
		this.project = project;
		this.hgRoot = root;
		setRule(new HgRootRule(hgRoot));
		updateRootIfProjectUnknown = true;
	}

	public RefreshStatusJob(String name, IProject project) {
		super(name);
		this.project = project;
		this.hgRoot = MercurialTeamProvider.getHgRoot(project);
		if(hgRoot != null) {
			setRule(new HgRootRule(hgRoot));
		}
	}

	public RefreshStatusJob(String name, HgRoot root) {
		super(name);
		this.hgRoot = root;
		this.project = null;
		setRule(new HgRootRule(root));
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if(monitor.isCanceled()){
			return Status.CANCEL_STATUS;
		}
		try {
			monitor.beginTask(Messages.refreshStatusJob_OptainingMercurialStatusInformation, 5);
			if(project != null && !updateRootIfProjectUnknown) {
				MERCURIAL_STATUS_CACHE.refreshStatus(project, monitor);
			} else {
				if(updateRootIfProjectUnknown){
					if(MercurialStatusCache.getInstance().isStatusKnown(project)){
						return Status.OK_STATUS;
					}
				}
				MERCURIAL_STATUS_CACHE.refreshStatus(hgRoot, monitor);
			}
		} catch (TeamException e) {
			MercurialEclipsePlugin.logError(e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	@Override
	public boolean belongsTo(Object family) {
		return RefreshStatusJob.class == family;
	}

	@Override
	public boolean shouldSchedule() {
		if(hgRoot == null){
			// probably should never happen except if we are running on a project
			// which was just created or just removed
			return true;
		}
		Job[] jobs = Job.getJobManager().find(RefreshStatusJob.class);
		for (Job job : jobs) {
			HgRootRule rule = (HgRootRule) job.getRule();
			if(hgRoot.equals(rule.getHgRoot()) && (job.getState() == WAITING || job.getState() == SLEEPING)){
				return false;
			}
		}
		if(updateRootIfProjectUnknown){
			return !MercurialStatusCache.getInstance().isStatusKnown(project);
		}
		return true;
	}
}