/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model.resources;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.team.internal.core.TeamPlugin;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

@SuppressWarnings("restriction")
public class HgProjectAdapter extends HgContainerAdapter implements IProject {

	public HgProjectAdapter(HgRoot root) {
		super(root, root, null);
		try {
			setSessionProperty(TeamPlugin.PROVIDER_PROP_KEY, new MercurialTeamProvider());
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public void build(int kind, IProgressMonitor monitor) throws CoreException {
		return;
	}

	public void close(IProgressMonitor monitor) throws CoreException {
		return;
	}

	public void create(IProjectDescription description, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void create(IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
		return Platform.getContentTypeManager();
	}

	public IProjectDescription getDescription() throws CoreException {
		ProjectDescription description = new ProjectDescription();
		description.setLocation(getLocation());
		description.setName(getName());
		return description;
	}


	public IProjectNature getNature(String natureId) throws CoreException {
		return null;
	}

	@SuppressWarnings("deprecation")
	public IPath getPluginWorkingLocation(org.eclipse.core.runtime.IPluginDescriptor plugin) {
		if (plugin == null) {
			return null;
		}
		return getWorkingLocation(plugin.getUniqueIdentifier());
	}

	public IPath getWorkingLocation(String id) {
		if (id == null || !exists()) {
			return null;
		}
		IPath stateLocation = ResourcesPlugin.getPlugin().getStateLocation();
		IPath result = stateLocation.append(".projects").append(getName()).append(id);
		result.toFile().mkdirs();
		return result;
	}


	public void move(IProjectDescription description, boolean force, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}


	public int getType() {
		return IResource.PROJECT;
	}

	@Override
	public IPath getFullPath() {
		return new Path(getName());
	}

	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IProject.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	public IProject[] getReferencedProjects() throws CoreException {
		return new IProject[0];
	}

	public IProject[] getReferencingProjects() {
		return new IProject[0];
	}

	public boolean hasNature(String natureId) throws CoreException {
		return false;
	}

	public boolean isNatureEnabled(String natureId) throws CoreException {
		return false;
	}

	public boolean isOpen() {
		return true;
	}

	public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
		open(monitor);
	}

	public void open(IProgressMonitor monitor) throws CoreException {
		return;
	}

	public void setDescription(IProjectDescription description, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void setDescription(IProjectDescription description, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	// Map/*<String, String>*/ is 3.6 API...
	public void build(int kind, String builderName, Map/*<String, String>*/ args,
			IProgressMonitor monitor) throws CoreException {
		throwCoreEx();
	}

	// 3.6 API
	public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor)
			throws CoreException {
		throwCoreEx();
	}

	// 3.7 API
	public IBuildConfiguration getActiveBuildConfig() throws CoreException {
		return throwCoreEx();
	}

	// 3.7 API
	public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
		return throwCoreEx();
	}

	// 3.7 API
	public IBuildConfiguration[] getBuildConfigs() throws CoreException {
		return throwCoreEx();
	}

	// 3.7 API
	public IBuildConfiguration[] getReferencedBuildConfigs(String configName, boolean includeMissing)
			throws CoreException {
		return throwCoreEx();
	}

	// 3.7 API
	public boolean hasBuildConfig(String configName) throws CoreException {
		return false;
	}

	// 3.6 API
	public void loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
			throws CoreException {
		throwCoreEx();
	}

	// 3.6 API
	public void saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
			throws CoreException {
		throwCoreEx();
	}

}
