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

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.internal.core.TeamPlugin;

import com.vectrace.MercurialEclipse.exception.HgCoreException;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public abstract class DummyResourceAdapter implements IResource {

	static <V> V throwEx() throws CoreException {
		throw new HgException("Unsupported operation");
	}

	static <V> V throwCoreEx() throws HgCoreException {
		throw new HgCoreException("Unsupported operation");
	}

	public boolean isDerived() {
		return false;
	}

	public boolean isDerived(int options) {
		return false;
	}

	public boolean isLinked() {
		return false;
	}

	public boolean isLinked(int options) {
		return false;
	}

	public boolean isLocal(int depth) {
		return true;
	}

	public boolean isPhantom() {
		return false;
	}

	public Map getPersistentProperties() throws CoreException {
		return Collections.EMPTY_MAP;
	}

	public String getPersistentProperty(QualifiedName key) throws CoreException {
		if(TeamPlugin.PROVIDER_PROP_KEY.equals(key)) {
			return MercurialTeamProvider.ID;
		}
		return null;
	}

	public boolean isSynchronized(int depth) {
		return true;
	}

	public boolean isTeamPrivateMember() {
		return false;
	}

	public boolean isTeamPrivateMember(int options) {
		return false;
	}

	public void clearHistory(IProgressMonitor monitor) throws CoreException {
		// noop
	}

	public void copy(IPath destination, boolean force, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void copy(IPath destination, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public IMarker createMarker(String type) throws CoreException {
		return throwEx();
	}

	public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
		throwEx();
	}

	public void move(IPath destination, boolean force, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void move(IPath destination, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void move(IProjectDescription description, boolean force, boolean keepHistory,
			IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void setDerived(boolean isDerived) throws CoreException {
		throwEx();
	}

	public void setHidden(boolean isHidden) throws CoreException {
		throwEx();
	}

	public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
		throwEx();
	}

	public void setReadOnly(boolean readOnly) {
		throwCoreEx();
		// 1.6!
		// file.setWritable(!readOnly);
	}

	public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
		throwCoreEx();
	}

	public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
		throwCoreEx();
	}

	public IMarker findMarker(long id) throws CoreException {
		return null;
	}

	public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth)
			throws CoreException {
		return new IMarker[0];
	}

	public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth)
			throws CoreException {
		return -1;
	}

	// 3.6 API
	public IPathVariableManager getPathVariableManager() {
		return null;
	}

	// 3.7 API
	public boolean isVirtual() {
		return false;
	}

	// 3.6 API
	public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
		throwCoreEx();
	}

}
