/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class RefreshStatusHandler extends MultipleResourcesHandler {

	@Override
	protected void run(List<IResource> resources) throws Exception {

		for (IResource res : resources) {
			MercurialRootCache.uncache(res);
		}

		if(resources.size() == 1){
			refreshSingleResource(resources.get(0));
			return;
		}

		// separate files or projects are selected
		Set<IResource> singleFiles = collectSingleFiles(resources);
		for (IResource resource : singleFiles) {
			refreshSingleResource(resource);
		}
		resources.removeAll(singleFiles);
		if(resources.isEmpty()){
			return;
		}
		Set<IProject> singleProjects = collectSingleProjects(resources);
		for (IProject project : singleProjects) {
			refreshSingleProject(project);
		}
		resources.removeAll(singleProjects);
		if(resources.isEmpty()){
			return;
		}
		for (HgRoot hgRoot : ResourceUtils.groupByRoot(resources).keySet()) {
			refreshRoot(hgRoot);
		}
	}

	static Set<IResource> collectSingleFiles(List<IResource> resources){
		Map<IProject, List<IResource>> byProject = ResourceUtils.groupByProject(resources);
		Set<Entry<IProject, List<IResource>>> resSet = byProject.entrySet();
		Set<IResource> singleResources = new HashSet<IResource>();
		for (Entry<IProject, List<IResource>> entry : resSet) {
			if(!resources.contains(entry.getKey())){
				singleResources.addAll(entry.getValue());
			}
		}

		// filter out resources contained in other resources
		Iterator<IResource> iterator = singleResources.iterator();
		while (iterator.hasNext()) {
			IResource resource = iterator.next();
			for (IResource other : singleResources) {
				if(other != resource && other.contains(resource)){
					iterator.remove();
					break;
				}
			}
		}
		return singleResources;
	}

	static Set<IProject> collectSingleProjects(List<IResource> resources){
		Set<IProject> singleProjects = new HashSet<IProject>();
		Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(resources);
		Set<Entry<HgRoot, List<IResource>>> entrySet = byRoot.entrySet();
		for (Entry<HgRoot, List<IResource>> entry : entrySet) {
			HgRoot hgRoot = entry.getKey();
			List<IProject> hgProjects = MercurialTeamProvider.getKnownHgProjects(hgRoot);
			List<IResource> selected = entry.getValue();
			if(selected.size() < hgProjects.size() / 2){
				singleProjects.addAll((Collection<? extends IProject>) selected);
			}
		}
		return singleProjects;
	}

	private static void refreshRoot(final HgRoot hgRoot) {
		new RefreshStatusJob("Refreshing hg root " + hgRoot.getName(), hgRoot).schedule();
	}

	private static void refreshSingleProject(final IProject project) {
		new RefreshStatusJob("Refreshing project " + project.getName(), project).schedule();
	}

	private static void refreshSingleResource(final IResource resource) {
		new Job(
				Messages.getString("RefreshStatusHandler.refreshingResource") + " " + resource.getName() + "...") { //$NON-NLS-1$ //$NON-NLS-2$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					MercurialStatusCache.getInstance().refreshStatus(resource, monitor);
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
					return e.getStatus();
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

}
