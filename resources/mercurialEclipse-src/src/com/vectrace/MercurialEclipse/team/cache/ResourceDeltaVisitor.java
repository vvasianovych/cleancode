/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov           - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbenchWindow;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.InitOperation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

final class ResourceDeltaVisitor implements IResourceDeltaVisitor {

	private final Map<IProject, RootResourceSet<IResource>> removed;
	private final Map<IProject, RootResourceSet<IResource>> changed;
	private final Map<IProject, RootResourceSet<IResource>> added;
	private final boolean autoShare;
	private final MercurialStatusCache cache;
	private int resourcesCount;
	private HgRoot currentRoot;

	ResourceDeltaVisitor(Map<IProject, RootResourceSet<IResource>> removed,
			Map<IProject, RootResourceSet<IResource>> changed,
			Map<IProject, RootResourceSet<IResource>> added) {
		this.removed = removed;
		this.changed = changed;
		this.added = added;
		cache = MercurialStatusCache.getInstance();
		autoShare = Boolean.valueOf(
				HgClients.getPreference(MercurialPreferenceConstants.PREF_AUTO_SHARE_PROJECTS, "false"))
				.booleanValue();
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource res = delta.getResource();
		if (res.getType() == IResource.ROOT) {
			return true;
		}
		final IProject project = res.getProject();

		// handle projects that contain a mercurial repository
		boolean openOrClosedOrDeleted = delta.getFlags() == IResourceDelta.OPEN
			|| delta.getKind() == IResourceDelta.REMOVED;

		if (autoShare && openOrClosedOrDeleted && project.isAccessible()
				&& RepositoryProvider.getProvider(project) == null) {
			autoshareProject(project);
			// stop tracking changes: after auto-share is completed, we will do a full refresh anyway
			return false;
		}

		if (!MercurialTeamProvider.isHgTeamProviderFor(res)) {
			return false;
		}

		// We should re-set currentRoot each time because the delta resources may be
		// from different projects AND from different UNRELATED roots too.
		currentRoot = MercurialTeamProvider.getHgRoot(res);

		if(currentRoot == null) {
			return true;
		}

		if((res == project && openOrClosedOrDeleted) || isCompleteStatusRequested()){
			addResource(changed, project, currentRoot, project);
			return false;
		}

		// NB: the resource may not exist at this point (deleted/moved)
		// so any access to the IResource's API should be checked against null
		if (res.getType() == IResource.FILE) {
			IResource resource = isCompleteStatusRequested()? project : res;
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				addResource(added, project, currentRoot, resource);
				// System.out.println("\t ADDED: " + resource);
				if(isCompleteStatusRequested()){
					return false;
				}
				break;
			case IResourceDelta.CHANGED:
				if (hasChangedBits(delta)
						&& cache.isSupervised(project, ResourceUtils.getPath(res))) {

					// fix for issue 10155: No status update after reverting changes on .hgignore
					if(MercurialStatusCache.canTriggerFullCacheUpdate(resource)){
						addResource(changed, project, currentRoot, project);
						return false;
					}
					addResource(changed, project, currentRoot, resource);
					// System.out.println("\t CHANGED: " + resource);
					if(isCompleteStatusRequested()){
						return false;
					}
				}
				break;
			case IResourceDelta.REMOVED:
				if (cache.isSupervised(project, ResourceUtils.getPath(res))) {
					addResource(removed, project, currentRoot, resource);
					// System.out.println("\t REMOVED: " + resource);
					if(isCompleteStatusRequested()){
						return false;
					}
				} else {
					// check the parent folder: if the file had "unknown" state, folder was
					// marked as "modified". Now the file is deleted, so we should try
					// to refresh the folder state
					res = res.getParent();
					if (res != null && cache.isSupervised(project, ResourceUtils.getPath(res))){
						resource = isCompleteStatusRequested()? project : res;
						addResource(changed, project, currentRoot, resource);
						// System.out.println("\t CHANGED: " + resource);
						if(isCompleteStatusRequested()){
							return false;
						}
					}
				}
				break;
			}
		}
		return true;
	}

	private boolean isCompleteStatusRequested() {
		return resourcesCount > MercurialStatusCache.NUM_CHANGED_FOR_COMPLETE_STATUS;
	}


	private void addResource(Map<IProject, RootResourceSet<IResource>> map, IProject project, HgRoot root, IResource res){
		if(root == null) {
			return;
		}
		RootResourceSet<IResource> set = map.get(project);
		if(set == null){
			set = new RootResourceSet<IResource>();
			map.put(project, set);
		}
		set.add(root, res);
		resourcesCount++;
	}

	private boolean hasChangedBits(IResourceDelta delta){
		return (delta.getFlags() & MercurialStatusCache.MASK_CHANGED) != 0;
	}

	private void autoshareProject(final IProject project) {
		final HgRoot hgRoot = MercurialRootCache.getInstance().calculateHgRoot(project);
		if(hgRoot == null){
			return;
		}
		MercurialEclipsePlugin.logInfo("Autosharing " + project.getName()
				+ ". Detected repository location: " + hgRoot, null);
		final IWorkbenchWindow activeWorkbenchWindow = MercurialEclipsePlugin.getActiveWindow();

		new SafeWorkspaceJob(NLS.bind(Messages.mercurialStatusCache_autoshare, project.getName())) {
			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				try {
					new InitOperation(activeWorkbenchWindow, project, hgRoot).run(monitor);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return super.runSafe(monitor);
			}
		}.schedule();
	}
}