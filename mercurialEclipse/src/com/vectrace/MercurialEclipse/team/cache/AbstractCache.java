/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This is the base class for 4 different caches we have in the current code:
 * <p>
 * Caches for local resources, do not maintain their state and has to be managed by clients:
 * <ul>
 * <li>{@link MercurialStatusCache} - maintains the Eclipse {@link IResource} states</li>
 * <li>{@link LocalChangesetCache} - maintains the known changesets in the local hg repository</li>
 * </ul>
 * <p>
 * Caches for remote resources and semi-automatically maintain their state:
 * <ul>
 * <li>{@link OutgoingChangesetCache} - maintains new changesets in the local hg repository</li>
 * <li>{@link IncomingChangesetCache} - maintains new changesets in the remote hg repository</li>
 * </ul>
 * @author bastian
 * @author Andrei Loskutov
 */
public abstract class AbstractCache extends Observable {

	protected static final SortedSet<ChangeSet> EMPTY_SET = Collections.unmodifiableSortedSet(new TreeSet<ChangeSet>());

	protected final boolean debug;

	public AbstractCache() {
		super();
		debug = MercurialEclipsePlugin.getDefault().isDebugging();
		final IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		configureFromPreferences(store);
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				configureFromPreferences(store);
			}
		});

		ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
					return;
				}
				try {
					ProjectDeltaVisitor visitor = new ProjectDeltaVisitor();
					event.getDelta().accept(visitor);
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}, IResourceChangeEvent.POST_CHANGE);
	}

	private class ProjectDeltaVisitor implements IResourceDeltaVisitor {

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource res = delta.getResource();
			if (res.getType() == IResource.ROOT) {
				return true;
			}
			if (res.getType() != IResource.PROJECT) {
				return false;
			}
			IProject project = (IProject) res;
			if (delta.getKind() == IResourceDelta.REMOVED
					|| ((delta.getFlags() & IResourceDelta.OPEN) != 0 && !project.isOpen())) {
				projectDeletedOrClosed(project);
			}
			return false;
		}
	}


	/**
	 * Clients has cleanup all caches related to given project.
	 */
	protected abstract void projectDeletedOrClosed(IProject project);

	/**
	 * does nothing, clients has to override and update preferences
	 */
	protected abstract void configureFromPreferences(IPreferenceStore store);

	/**
	 * Spawns an update job to notify all the clients about given resource changes
	 * @param resource non null
	 */
	protected void notifyChanged(final IResource resource, boolean expandMembers) {
		final Set<IResource> resources = new HashSet<IResource>();
		if(!expandMembers) {
			resources.add(resource);
		}
		notifyChanged(resources, expandMembers);
	}

	/**
	 * Spawns an update job to notify all the clients about given resource changes
	 * @param resources non null
	 */
	protected void notifyChanged(final Set<IResource> resources, final boolean expandMembers) {
		class ExclusiveRule implements ISchedulingRule {
			public boolean contains(ISchedulingRule rule) {
				return isConflicting(rule) || rule instanceof IResource || rule instanceof HgRootRule;
			}
			public boolean isConflicting(ISchedulingRule rule) {
				return rule instanceof ExclusiveRule;
			}
		}
		Job job = new Job("hg cache clients update..."){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Set<IResource> set;
				if(!expandMembers){
					set = resources;
				} else {
					set = new HashSet<IResource>(resources);
					for (IResource r : resources) {
						if(monitor.isCanceled()){
							return Status.CANCEL_STATUS;
						}
						set.addAll(ResourceUtils.getMembers(r));
					}
				}
				setChanged();
				notifyObservers(set);
				return Status.OK_STATUS;
			}
		};
		job.setRule(new ExclusiveRule());
		job.setSystem(true);
		job.schedule();
	}


	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
