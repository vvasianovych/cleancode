/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.resources.HgProjectAdapter;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;


/**
 * @author Andrei
 */
public final class HgRootContainer extends HgProjectAdapter {

	private boolean initDone;

	public HgRootContainer(HgRoot hgRoot) {
		super(hgRoot);
		init();
	}

	public void init() {
		// TODO implement REAL project/folders for each hg root
		// things to think about (inspired by ExternalFoldersManager in JDT):
		// - refresh on startup or first access
		// - cleanup on shutdown (to avoid endless growth of non existent roots)
		// - traversal of REAL tree instead of faked one in members() call
		if(true) {
			return;
		}
		if(initDone || !getHgRoot().exists()) {
			return;
		}

		final IProject project = getWorkspace().getRoot().getProject(".hg_repo_roots");
		try {
			if(!project.exists()) {
				IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
				IPath location = MercurialEclipsePlugin.getDefault().getStateLocation().append(project.getName());
				description.setLocation(location);
				project.create(description, IResource.HIDDEN, null);
			}
			if(!project.isOpen()) {
				project.open(IResource.BACKGROUND_REFRESH, null);
			}
			IFolder folder = project.getFolder(getName());
			if (!folder.exists()) {
				folder.createLink(getLocation(), IResource.ALLOW_MISSING_LOCAL, null);
			} else {
				folder.refreshLocal(DEPTH_INFINITE, null);
			}
			MercurialRootCache.markAsCached(folder, getHgRoot());
			MercurialRootCache.markAsCached(project, getHgRoot());
			initDone = true;
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

}