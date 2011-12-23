/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - Implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCloneClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialProjectSetCapability;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * This action adds projects to the workspace using their unique Mercurial
 * reference strings from projectsets
 *
 * @author Bastian Doetsch
 */
public class AddToWorkspaceAction extends WorkspaceModifyOperation {
	private String[] referenceStrings;
	private IProject[] projectsCreated;

	public AddToWorkspaceAction() {
		super();
	}

	public AddToWorkspaceAction(ISchedulingRule rule) {
		super(rule);
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {

		try {
			monitor.beginTask("Adding projects to workspace...", referenceStrings.length);
			IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
			ArrayList<IProject> projects = new ArrayList<IProject>(referenceStrings.length);

			/*
			 * iterate over all reference strings and use them to create
			 * projects in the current workspace.
			 *
			 * A reference string uses underscore as delimiter and looks like
			 * this:
			 *
			 * "MercurialEclipseProjectSet|ProjectName|RepositoryURLForClone"
			 *
			 */
			for (String reference : referenceStrings) {
				if (monitor.isCanceled()) {
					break;
				}

				MercurialProjectSetCapability psc = MercurialProjectSetCapability.getInstance();

				// Project name is stored in part 1
				String projectName = psc.getProject(reference);
				IProject proj = wsRoot.getProject(projectName);

				// only new projects
				if (proj.exists() || proj.getLocation() != null) {
					MercurialEclipsePlugin.logInfo("Project" + proj.getName()
							+ " not imported. Already exists.", null);
					monitor.worked(1);
					continue;
				}

				String rootRelativePath = psc.getRootRelativePath(reference);
				if(rootRelativePath != null){
					MercurialEclipsePlugin.logInfo("Project" + proj.getName()
							+ " not imported, as it was only a part of the hg repo.", null);
					// TODO somehow allow to clone multiple projects from ONE hg root
					break;
				}

				// Repository-URL is stored in part 2
				HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
				IHgRepositoryLocation location = repoManager.getRepoLocation(psc
						.getPullRepo(reference), null, null);

				HgCloneClient.clone(wsRoot.getLocation().toFile(), location, false, false, false,
						false, null, projectName);

				proj.create(monitor);
				proj.open(monitor);

				// Register the project with Team.
				RepositoryProvider.map(proj, MercurialTeamProvider.class.getName());
				RepositoryProvider.getProvider(proj, MercurialTeamProvider.class.getName());
				projects.add(proj);

				HgRoot hgRoot = MercurialTeamProvider.getHgRoot(proj);
				if (hgRoot != null) {
					// store repo (will be set as default automatically)
					repoManager.addRepoLocation(hgRoot, location);
				}

				// refresh project to get decorations
				proj.refreshLocal(IResource.DEPTH_INFINITE, monitor);

				// increase monitor so we see at least a bit of a progress when
				// importing multiple projects
				monitor.worked(1);
			}
			projectsCreated = projects.toArray(new IProject[projects.size()]);
		} finally {
			monitor.done();
		}
	}

	public String[] getReferenceStrings() {
		return referenceStrings;
	}

	public void setReferenceStrings(String[] referenceStrings) {
		this.referenceStrings = referenceStrings;
	}

	public IProject[] getProjectsCreated() {
		return projectsCreated;
	}

	public void setProjectsCreated(IProject[] projectsCreated) {
		this.projectsCreated = projectsCreated;
	}
}
