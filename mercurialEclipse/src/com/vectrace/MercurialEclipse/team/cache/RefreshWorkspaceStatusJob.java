/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public final class RefreshWorkspaceStatusJob extends RefreshRootJob {

	private final boolean refreshWorkspaceOnly;

	/**
	 * Refreshes all the projects of given root in the workspace. Does NOT refresh any
	 * cache
	 * @param root non null
	 */
	public RefreshWorkspaceStatusJob(HgRoot root) {
		this(root, WORKSPACE);
	}

	/**
	 * Refreshes all the projects of given root in the workspace. If the flags are non zero,
	 * additionally refreshes different mercurial caches for the root.
	 * @param root non null
	 * @param flags see {@link RefreshRootJob} flags. Zero flag value does nothing.
	 */
	public RefreshWorkspaceStatusJob(HgRoot root, int flags) {
		super(root, flags);
		this.refreshWorkspaceOnly = flags == WORKSPACE;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			String branch = null;
			if(!refreshWorkspaceOnly) {
				trace("BRANCH");
				branch = HgBranchClient.getActiveBranch(hgRoot);
				// update branch name
				MercurialTeamProvider.setCurrentBranch(branch, hgRoot);
			}

			Set<IProject> projects = ResourceUtils.getProjects(hgRoot);

			if(!projects.isEmpty()){
				trace(projects.size() + " WORKSPACE projects");
			}

			for (IProject project : projects) {
				if(monitor.isCanceled()){
					return Status.CANCEL_STATUS;
				}
				refreshProject(monitor, project);
			}

			if(!refreshWorkspaceOnly){
				return super.run(monitor);
			}
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	private void refreshProject(IProgressMonitor monitor, IProject project) throws CoreException {
		if(!refreshWorkspaceOnly){
			// reset merge properties
			MercurialStatusCache.getInstance().clearMergeStatus(project);
		}

		// refresh resources
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}

	@Override
	public boolean belongsTo(Object family) {
		return RefreshWorkspaceStatusJob.class == family || super.belongsTo(family);
	}

	@Override
	public boolean shouldSchedule() {
		Job[] jobs = Job.getJobManager().find(RefreshWorkspaceStatusJob.class);
		for (Job job : jobs) {
			if(isSimilar(job)){
				// do not schedule me because exactly the same job is waiting to be started!
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean isSimilar(Job job) {
		if(!(job instanceof RefreshWorkspaceStatusJob)){
			return false;
		}
		return super.isSimilar(job);
	}
}