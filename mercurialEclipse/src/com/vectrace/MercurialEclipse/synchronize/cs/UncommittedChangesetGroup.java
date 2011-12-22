/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * The group containing both "dirty" files as also not yet committed changesets
 * <p>
 * This group CAN observe changes of the status cache. If it is added as a listener
 * to the {@link MercurialStatusCache}, it only tracks the state of the enabled (root)
 * projects (see {@link UncommittedChangesetManager#getProjects()}).
 *
 * @author Andrei
 */
public class UncommittedChangesetGroup extends ChangesetGroup implements Observer {


	private final List<IPropertyChangeListener> listeners;
	private final PropertyChangeEvent event;
	private HgSubscriberMergeContext context;

	private volatile boolean updateRequired;
	private volatile boolean cachingOn;

	private final MercurialStatusCache cache;
	private final Set<IFile> files;
	private final UncommittedChangesetManager ucsManager;
	private static final String DEFAULT_NAME = "New changeset";

	public UncommittedChangesetGroup(UncommittedChangesetManager ucsManager) {
		super("Uncommitted", Direction.LOCAL);
		this.ucsManager = ucsManager;
		cache = MercurialStatusCache.getInstance();
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		event = new PropertyChangeEvent(this, "", null, "");
		files = new HashSet<IFile>();
	}

	public void addListener(IPropertyChangeListener listener){
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(IPropertyChangeListener listener){
		listeners.remove(listener);
	}

	public void setContext(HgSubscriberMergeContext context) {
		this.context = context;
	}

	public void dispose() {
		cache.deleteObserver(this);
		clear();
	}

	public void clear(){
		Set<IFile> files2 = ucsManager.getDefaultChangeset().getFiles();
		files.removeAll(files2);
		ucsManager.getDefaultChangeset().clear();
		Set<ChangeSet> set = getChangesets();
		for (ChangeSet cs : set) {
			Iterator<IFile> iterator = cs.getFiles().iterator();
			while(iterator.hasNext()) {
				IFile file = iterator.next();
				if(cache.isClean(file)) {
					iterator.remove();
				}
			}
		}
	}


	private boolean add(IFile file){
		return add(file, ucsManager.getDefaultChangeset());
	}

	public boolean contains(IFile file) {
		return files.contains(file);
	}

	public boolean add(IFile file, WorkingChangeSet set){
		if(!getChangesets().contains(set)) {
			return false;
		}
		if(context != null && context.isHidden(file)){
			return false;
		}

		if(cache.isDirectory(file.getLocation())){
			return false;
		}
		boolean added;
		synchronized (files){
			added = files.add(file);
		}
		if(added) {
			// update files in the given changeset
			set.add(file);

			// we need only one event
			if(cachingOn){
				updateRequired = true;
			} else {
				notifyListeners();
			}
		}
		return added;
	}

	public boolean add(WorkingChangeSet cs) {
		if(getChangesets().contains(cs)) {
			return false;
		}
		getChangesets().add(cs);
		Set<IFile> files2 = cs.getFiles();
		for (IFile file : files2) {
			if(!files.add(file)) {
				cs.remove(file);
			}
		}
		changesetChanged(cs);
		return true;
	}

	public void committed(WorkingChangeSet cs) {
		Set<IFile> set = new LinkedHashSet<IFile>(cs.getFiles());
		for (IFile file : set) {
			remove(file, cs);
		}
		if(cs.isDefault()) {
			cs.setName(generateNewChangesetName());
			cs.setComment("");
			changesetChanged(cs);
			return;
		}
		getChangesets().remove(cs);
		move(cs.getFiles().toArray(new IFile[0]), ucsManager.getDefaultChangeset());
	}

	public boolean delete(WorkingChangeSet cs) {
		if(cs.isDefault()) {
			return false;
		}
		getChangesets().remove(cs);
		move(cs.getFiles().toArray(new IFile[0]), ucsManager.getDefaultChangeset());
		return true;
	}

	public WorkingChangeSet create(IFile[] filesToAdd) {
		WorkingChangeSet cs = new WorkingChangeSet(generateNewChangesetName(), this);
		move(filesToAdd, cs);
		return cs;
	}

	/**
	 * @return
	 */
	private String generateNewChangesetName() {
		Set<ChangeSet> allSets = getChangesets();
		int count = 0;
		String name = DEFAULT_NAME;
		main: for (ChangeSet cs : allSets) {
			if(cs.getName().equals(name)) {
				if(count > 10000) {
					// stop looping, user is crazy anyway
					return DEFAULT_NAME;
				}
				count ++;
				name = DEFAULT_NAME + " #" + count;
				continue main;
			}
		}
		return name;
	}

	public void move(IFile[] files1, WorkingChangeSet to){
		Set<ChangeSet> changesets = getChangesets();
		for (ChangeSet cs : changesets) {
			for (IFile file : files1) {
				if (cs.contains(file)) {
					((WorkingChangeSet)cs).removeFile(file);
				}
			}
		}
		if(!changesets.contains(to)) {
			changesets.add(to);
		}
		for (IFile file : files1) {
			to.add(file);
		}
		changesetChanged(to);
	}

	public void remove(IResource file, WorkingChangeSet set){
		if(file instanceof IFile) {
			set.removeFile((IFile) file);
		}
		files.remove(file);
	}

	/**
	 * TODO currently unused but initially implemented for the issue 10732
	 * @param paths
	 */
	protected void hide(IPath[] paths){
		if(context == null){
			return;
		}
		boolean changed = false;
		MercurialStatusCache statusCache = MercurialStatusCache.getInstance();
		Set<IProject> projects = getProjectSet();

		for (IPath path : paths) {
			if(path.segmentCount() < 2){
				continue;
			}
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(path.segment(0));
			if(project == null || !projects.contains(project)){
				continue;
			}
			IResource res = project.findMember(path.removeFirstSegments(1));
			// only allow to hide files which are dirty
			if(res instanceof IFile && !statusCache.isClean(res)){
				IFile file = (IFile) res;
				synchronized (files) {
					if(files.contains(file)){
						context.hide(file);
						files.remove(file);
						changed = true;
					}
				}
			}
		}
		if(changed){
			updateRequired = true;
			endInput(null);
		}
	}

	private Set<IProject> getProjectSet() {
		Set<IProject> projects = new HashSet<IProject>();
		if(ucsManager.getProjects() != null){
			projects.addAll(Arrays.asList(ucsManager.getProjects()));
		}
		return projects;
	}

	private void beginInput() {
		cachingOn = true;
	}

	private void endInput(IProgressMonitor monitor) {
		cachingOn = false;
		if(!updateRequired){
			return;
		}
		updateRequired = false;
		notifyListeners();
	}

	private void update(Set<IProject> projectSet){
		boolean changed = false;
		try {
			beginInput();
			clear();
			for (IProject project : projectSet) {
				changed |= update(project);
			}
		} finally {
			updateRequired |= changed;
			endInput(null);
		}
	}

	private boolean update(IProject project){
		Set<IProject> projects = getProjectSet();
		if(!projects.contains(project)){
			return false;
		}
		final int bits = MercurialStatusCache.MODIFIED_MASK;
		Set<IFile> files2 = cache.getFiles(bits, project);
		if(files2.isEmpty()){
			return true;
		}
		boolean changed = false;
		for (IFile file : files2) {
			changed |= add(file);
		}
		return changed;
	}

	public void changesetChanged(WorkingChangeSet set) {
		// TODO: add argument to avoid too much updates?
		ucsManager.storeChangesets();
//		ucsManager.assignRemainingFiles();
		notifyListeners();
	}

	private void notifyListeners() {
		Job updateJob = new Job("Uncommitted changeset update"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (IPropertyChangeListener listener : listeners) {
					listener.propertyChange(event);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
			@Override
			public boolean belongsTo(Object family) {
				return family == ExclusiveRule.class;
			}

			@Override
			public boolean shouldSchedule() {
				Job[] jobs = Job.getJobManager().find(ExclusiveRule.class);
				for (Job job : jobs) {
					ExclusiveRule rule = (ExclusiveRule) job.getRule();
					if(UncommittedChangesetGroup.this.equals(rule.cs)){
						// do not schedule me because exactly the same job is waiting to be started!
						return false;
					}
				}
				return true;
			}
		};
		updateJob.setRule(new ExclusiveRule(this));
		updateJob.schedule(50);
	}

	private final class ExclusiveRule implements ISchedulingRule {
		private final UncommittedChangesetGroup cs;

		public ExclusiveRule(UncommittedChangesetGroup cs) {
			this.cs = cs;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExclusiveRule && cs.equals(((ExclusiveRule)rule).cs);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	public void update(Observable o, Object arg) {
		update(getProjectSet());
	}
}
