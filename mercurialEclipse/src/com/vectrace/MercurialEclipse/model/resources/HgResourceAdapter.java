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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

import com.vectrace.MercurialEclipse.model.HgPath;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public abstract class HgResourceAdapter extends DummyResourceAdapter implements IHgResource {

	private final File file;
	private final IPath path;
	private final HgRoot root;
	private final IContainer parent;
	private final Map<QualifiedName,Object> sessionProps;

	public HgResourceAdapter(File file, HgRoot root, HgContainerAdapter parent) {
		this.file = file;
		this.root = root;
		this.parent = parent == null? getWorkspace().getRoot() : parent;
		this.path = file instanceof HgPath? ((HgPath)file).getIPath() : new Path(file.getAbsolutePath());
		this.sessionProps = new HashMap<QualifiedName,Object>();
	}

	@Override
	public String toString() {
		return file.toString();
	}

	public HgRoot getHgRoot() {
		return root;
	}

	public File toFile() {
		return file;
	}

	public IResourceProxy createProxy() {
		return new ResourceProxyAdapter(this);
	}

	public void accept(IResourceVisitor visitor) throws CoreException {
		accept(visitor, IResource.DEPTH_INFINITE, 0);
	}

	public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms)
			throws CoreException {
		accept(visitor, depth, includePhantoms ? IContainer.INCLUDE_PHANTOMS : 0);
	}

	public boolean exists() {
		return file.exists();
	}

	public IPath getFullPath() {
		int matchingFirstSegments = getLocation().matchingFirstSegments(getProject().getLocation());
		return getLocation().removeFirstSegments(matchingFirstSegments - 1);
	}

	public long getLocalTimeStamp() {
		return file.lastModified();
	}

	public IPath getLocation() {
		return path;
	}

	public URI getLocationURI() {
		return file.toURI();
	}

	public IMarker getMarker(long id) {
		return throwCoreEx();
	}

	public long getModificationStamp() {
		return getLocalTimeStamp();
	}

	public String getName() {
		return file.getName();
	}

	public final IContainer getParent() {
		return parent;
	}

	public final IProject getProject() {
		return getHgRoot().getResource();
	}

	public IPath getProjectRelativePath() {
		return getFullPath().removeFirstSegments(1);
	}

	public IPath getRawLocation() {
		return getLocation();
	}

	public URI getRawLocationURI() {
		return getLocationURI();
	}

	public ResourceAttributes getResourceAttributes() {
		ResourceAttributes ra = new ResourceAttributes();
		ra.setArchive(true);
		ra.setSymbolicLink(false);
		ra.setExecutable(file.canExecute());
		ra.setHidden(file.isHidden());
		ra.setReadOnly(!file.canWrite());
		return ra;
	}

	public Map getSessionProperties() throws CoreException {
		return sessionProps;
	}

	public Object getSessionProperty(QualifiedName key) throws CoreException {
		return sessionProps.get(key);
	}

	public IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public boolean isAccessible() {
		return exists();
	}

	public boolean isHidden() {
		return file.isHidden();
	}

	public boolean isHidden(int options) {
		return isHidden();
	}

	public boolean isReadOnly() {
		return !file.canWrite();
	}

	public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
		MercurialStatusCache.getInstance().refreshStatus(this, monitor);
	}

	public void touch(IProgressMonitor monitor) throws CoreException {
		MercurialStatusCache.getInstance().refreshStatus(this, monitor);
	}

	public void revertModificationStamp(long value) throws CoreException {
		file.setLastModified(value);
	}

	public long setLocalTimeStamp(long value) throws CoreException {
		file.setLastModified(value);
		return getLocalTimeStamp();
	}

	public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
		sessionProps.put(key, value);
	}

	public Object getAdapter(Class adapter) {
		if(adapter == IResource.class) {
			return this;
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public boolean contains(ISchedulingRule rule) {
		if (this == rule) {
			return true;
		}
		if (rule instanceof MultiRule) {
			MultiRule multi = (MultiRule) rule;
			ISchedulingRule[] children = multi.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (!contains(children[i])) {
					return false;
				}
			}
			return true;
		}
		if (!(rule instanceof IResource)) {
			return false;
		}
		return getLocation().isPrefixOf(((IResource) rule).getLocation());
	}

	public boolean isConflicting(ISchedulingRule rule) {
		if (!(rule instanceof IResource)) {
			return false;
		}
		IPath otherPath = ((IResource) rule).getLocation();
		return getLocation().isPrefixOf(otherPath) || otherPath.isPrefixOf(getLocation());
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HgResourceAdapter)) {
			return false;
		}
		HgResourceAdapter other = (HgResourceAdapter) obj;
		return file.equals(other.file);
	}

}
