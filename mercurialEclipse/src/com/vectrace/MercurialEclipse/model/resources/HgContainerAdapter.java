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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.model.HgRoot;

public abstract class HgContainerAdapter extends HgResourceAdapter implements IContainer {

	public HgContainerAdapter(File file, HgRoot root, HgContainerAdapter parent) {
		super(file, root, parent);
	}

	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IContainer.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
		boolean ok = visitor.visit(createProxy());
		if(ok) {
			IResource[] members = members();
			for (IResource resource : members) {
				resource.accept(visitor, memberFlags);
			}
		}
	}

	public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
		boolean ok = visitor.visit(this);
		if(ok) {
			IResource[] members = members();
			for (IResource resource : members) {
				resource.accept(visitor, depth, memberFlags);
			}
		}
	}

	public boolean exists(IPath path) {
		return getLocation().append(path).toFile().exists();
	}

	public IResource findMember(String name) {
		return findMember(getLocation().append(name));
	}

	public IResource findMember(String name, boolean includePhantoms) {
		return findMember(name);
	}

	public IResource findMember(IPath path, boolean includePhantoms) {
		return findMember(path);
	}

	public IResource findMember(IPath path) {
		if(path.isEmpty()) {
			return this;
		}
		File member = getLocation().append(path).toFile();
		if (!member.exists()) {
			return null;
		}
		if (member.isFile()) {
			return getFile(path);
		}
		return getFolder(path);
	}

	public IFile getFile(IPath path) {
		return getChild(IFile.class, path);
	}

	public IFolder getFolder(IPath path) {
		return getChild(IFolder.class, path);
	}

	private <V> V getChild(Class<V> clazz, IPath child) {
		if(child.isEmpty()) {
			if(!clazz.isAssignableFrom(getClass())) {
				return null;
			}
			return clazz.cast(this);
		}

		String name = child.segment(0);

		File memberFile = getLocation().append(name).toFile();
		if (!memberFile.exists()) {
			return null;
		}

		if (child.segmentCount() == 1) {
			if (memberFile.isFile()) {
				if (clazz == IFile.class) {
					return clazz.cast(getFile(name));
				}
				return null;
			} else if(clazz == IFolder.class) {
				return clazz.cast(getFolder(name));
			} else {
				return null;
			}
		}

		IFolder folder = getFolder(name);
		child = child.removeFirstSegments(1);
		return ((HgContainerAdapter) folder).getChild(clazz, child);
	}

	public IFile getFile(String name) {
		File member = getLocation().append(name).toFile();
		return new HgFileAdapter(member, getHgRoot(), this);
	}

	public IFolder getFolder(String name) {
		File member = getLocation().append(name).toFile();
		return new HgFolderAdapter(member, getHgRoot(), this);
	}

	public IResource[] members() throws CoreException {
		File[] files = toFile().listFiles();
		if(files == null || files.length == 0) {
			return new IResource[0];
		}
		List<IResource> members = new ArrayList<IResource>(files.length);
		// Intentionally NOT using MercurialTeamProvider.getProjects(getRoot())
		// to get ALL projects containing in this root, even if they are not configured yet
		IProject[] projects = null;
		IPath[] projectLocations = null;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				members.add(new HgFileAdapter(files[i], getHgRoot(), this));
			} else {
				if(files[i].getName().equals(".hg")) {
					continue;
				}
				if (projects == null) {
					projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
					projectLocations = new IPath[projects.length];
					int j = 0;
					for (IProject project : projects) {
						projectLocations[j++] = project.getLocation();
					}
				}
				HgFolderAdapter folder = new HgFolderAdapter(files[i], getHgRoot(), this);
				IProject project = getProject(folder, projects, projectLocations);
				if(project != null) {
					members.add(project);
				} else {
					members.add(folder);
				}
			}
		}
		return members.toArray(new IResource[members.size()]);
	}

	static IProject getProject(HgFolderAdapter folder, IProject[] projects, IPath[] projectLocations) {
		int i = 0;
		for (IProject project : projects) {
			if(folder.getLocation().equals(projectLocations[i++])) {
				return project;
			}
		}
		return null;
	}

	public final String getDefaultCharset() throws CoreException {
		return getHgRoot().getEncoding();
	}

	public String getDefaultCharset(boolean checkImplicit) throws CoreException {
		return getDefaultCharset();
	}

	public IResource[] members(boolean includePhantoms) throws CoreException {
		return members();
	}

	public IResource[] members(int memberFlags) throws CoreException {
		return members();
	}

	public String getFileExtension() {
		return null;
	}

	public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor)
			throws CoreException {
		return new IFile[0];
	}

	public void setDefaultCharset(String charset) throws CoreException {
		throwEx();
	}

	public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	// 3.6 API
	public IResourceFilterDescription createFilter(int type,
			FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		return throwCoreEx();
	}

	// 3.6 API
	public IResourceFilterDescription[] getFilters() throws CoreException {
		return new IResourceFilterDescription[0];
	}
}
