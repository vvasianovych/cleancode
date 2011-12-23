/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov         - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.util.IPropertyChangeListener;

import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * A temporary changeset which holds not commited resources. This changeset cannot be used
 * as a usual changeset, as many of it's functionality is not supported or limited.
 * <p>
 * This changeset CAN observe changes of the status cache. If it is added as a listener
 * to the {@link MercurialStatusCache}, it only tracks the state of the enabled (root)
 * projects (see {@link #setRoots(IProject[])}).
 *
 * @author Andrei
 */
public class WorkingChangeSet extends ChangeSet {

	private final List<IPropertyChangeListener> listeners;
	private final Set<IProject> projects;
	private boolean isDefault;

	private final UncommittedChangesetGroup group;

	public WorkingChangeSet(String name, UncommittedChangesetGroup group) {
		super(-1, name, null, null, "", null, "", null, null); //$NON-NLS-1$
		this.group = group;
		direction = Direction.OUTGOING;
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		projects = new LinkedHashSet<IProject>();
		files = new LinkedHashSet<IFile>();
		setName(name);
		group.add(this);
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	/**
	 * @return true if all changes should go to this changeset first (if there are more then one
	 *         uncommitted changeset available)
	 */
	public boolean isDefault() {
		return isDefault;
	}

	public boolean add(IFile file){
		boolean contains = group.contains(file);
		boolean added = contains;
		if(!contains) {
			added = group.add(file, this);
		}
		if(added) {
			synchronized (files){
				added = files.add(file);
			}
		}
		return added;
	}

	@Override
	public Set<IFile> getFiles() {
		return Collections.unmodifiableSet(files);
	}

	public void removeFile(IFile file) {
		// TODO check group files
//		boolean contains = group.contains(file);
//		boolean added = contains;
		synchronized (files){
			files.remove(file);
		}
	}

	@Override
	public void remove(IResource file){
		// simply not supported, as it may be called not only from our code
	}

	public void addListener(IPropertyChangeListener listener){
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(IPropertyChangeListener listener){
		listeners.remove(listener);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		String changeset = getChangeset();
		int size = getFiles().size();
		if(size == 0){
			return changeset + " (empty)";
		}
		return changeset + " (" + size + ")";
	}

	public void clear(){
		synchronized (files){
			files.clear();
		}
	}

	/**
	 * @param projects non null project list the changeset is responsible for
	 */
	public void setRoots(IProject[] projects) {
		synchronized (this.projects) {
			this.projects.clear();
			for (IProject project : projects) {
				this.projects.add(project);
				HgRoot root = MercurialTeamProvider.getHgRoot(project);
				if(root != null) {
					this.projects.add(root.getResource());
				}
			}
		}
	}

	public void dispose() {
		clear();
		synchronized (projects) {
			projects.clear();
		}
	}

	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Override
	public FileFromChangeSet[] getChangesetFiles() {
		Set<IFile> files2 = getFiles();
		int diffKind = Differencer.CHANGE | Differencer.RIGHT;

		List<FileFromChangeSet> fcs = new ArrayList<FileFromChangeSet>(files2.size());
		for (IFile file : files2) {
			fcs.add(new FileFromChangeSet(this, file, null, diffKind));
		}
		return fcs.toArray(new FileFromChangeSet[0]);
	}

	/**
	 * @return the group, never null
	 */
	public UncommittedChangesetGroup getGroup() {
		return group;
	}

	private final class ExclusiveRule implements ISchedulingRule {
		private final WorkingChangeSet cs;

		public ExclusiveRule(WorkingChangeSet cs) {
			this.cs = cs;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExclusiveRule && cs.equals(((ExclusiveRule) rule).cs);
		}
	}


}