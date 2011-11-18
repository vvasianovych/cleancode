/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.subscribers.IChangeSetChangeListener;
import org.eclipse.team.internal.ui.synchronize.SyncInfoSetChangeSetCollector;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IPropertyListener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.AbstractRemoteCache;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class HgChangesetsCollector extends SyncInfoSetChangeSetCollector {

	private final MercurialSynchronizeParticipant participant;
	private final IPropertyListener branchListener;
	private static final Set<ChangeSet> EMPTY_SET = Collections.unmodifiableSet(new HashSet<ChangeSet>());

	private final class ChangesetsCollectorJob extends Job {

		private ChangesetsCollectorJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Get remote hg data", 2);
			Set<ChangeSet> oldSets = getChangeSets();
			Set<ChangeSet> newSets;
			AbstractRemoteCache in = IncomingChangesetCache.getInstance();
			AbstractRemoteCache out = OutgoingChangesetCache.getInstance();
			int mode = getConfiguration().getMode();
			switch (mode) {
			case ISynchronizePageConfiguration.INCOMING_MODE:
				monitor.subTask("Collecting incoming changesets");
				newSets = initRemote(in);
				updateChangesets(oldSets, newSets, null);
				monitor.internalWorked(2);
				break;
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				monitor.subTask("Collecting outgoing changesets");
				newSets = initRemote(out);
				updateChangesets(oldSets, newSets, null);
				monitor.internalWorked(2);
				break;
			case ISynchronizePageConfiguration.BOTH_MODE:
				monitor.subTask("Collecting outgoing changesets");
				newSets = initRemote(out);
				updateChangesets(oldSets, newSets, Direction.OUTGOING);
				monitor.internalWorked(1);

				monitor.subTask("Collecting incoming changesets");
				newSets = initRemote(in);
				updateChangesets(oldSets, newSets, Direction.INCOMING);
				monitor.internalWorked(1);
				break;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				monitor.subTask("Collecting outgoing changesets");
				newSets = initRemote(out);
				monitor.internalWorked(1);

				monitor.subTask("Collecting incoming changesets");
				newSets.addAll(initRemote(in));
				monitor.internalWorked(1);

				newSets = retainConflicts(newSets);
				updateChangesets(oldSets, newSets, null);
				break;
			default:
				break;
			}

			monitor.done();
			return Status.OK_STATUS;
		}
	}

	private void updateChangesets(Set<ChangeSet> oldSets, Set<ChangeSet> newSets, Direction direction){
		Set<ChangeSet> removed = new HashSet<ChangeSet>();
		for (ChangeSet changeSet : oldSets) {
			if(direction != null && direction != changeSet.getDirection()){
				continue;
			}
			if(!newSets.contains(changeSet)){
				remove(changeSet);
				removed.add(changeSet);
			}
		}

		for (ChangeSet changeSet : newSets) {
			if(!oldSets.contains(changeSet)){
				add(changeSet);
			}
		}

		oldSets.removeAll(removed);
	}

	private static class ExclusiveRule implements ISchedulingRule {
		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}
		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExclusiveRule;
		}
	}

	public HgChangesetsCollector(ISynchronizePageConfiguration configuration) {
		super(configuration);
		this.participant = (MercurialSynchronizeParticipant) configuration.getParticipant();
		branchListener = new IPropertyListener() {
			public void propertyChanged(Object source, int propId) {
				if(getScope().getChangesetProvider().isParticipantCreated()) {
					branchChanged((HgRoot) source);
				}
			}
		};
		MercurialTeamProvider.addBranchListener(branchListener);
		getSubscriber().setCollector(this);
	}


	protected void branchChanged(HgRoot hgRoot) {
		MercurialSynchronizeSubscriber subscriber = getSubscriber();
		IProject[] projects = subscriber.getProjects();
		boolean needUpdate = false;
		for (IProject project : projects) {
			if(hgRoot.equals(MercurialTeamProvider.hasHgRoot(project))){
				needUpdate = true;
				break;
			}
		}
		if(needUpdate){
			initializeSets();
		}
	}

	@Override
	protected void add(SyncInfo[] infos) {
		// noop
	}

	@Override
	protected void initializeSets() {
		Job job = new ChangesetsCollectorJob("Initializing changesets");
		job.setRule(new ExclusiveRule());
		job.schedule(100);
	}

	private Set<ChangeSet> retainConflicts(Set<ChangeSet> newSets) {
		// TODO let only changesets with conflicting changes
		return newSets;
	}

	private Set<ChangeSet> initRemote(final AbstractRemoteCache cache) {
		MercurialSynchronizeSubscriber subscriber = getSubscriber();
		final IProject[] projects = subscriber.getProjects();
		if(projects.length == 0){
			return EMPTY_SET;
		}

		final IHgRepositoryLocation repo = participant.getRepositoryLocation();
		final Set<ChangeSet> result = new HashSet<ChangeSet>();

		Runnable runnable = new Runnable() {
			public void run() {
				for (IProject project : projects) {
					HgRoot hgRoot = MercurialTeamProvider.getHgRoot(project);
					if(hgRoot == null){
						continue;
					}
					String syncBranch = MercurialSynchronizeSubscriber.getSyncBranch(hgRoot);
					try {
						result.addAll(cache.getChangeSets(project, repo, syncBranch));
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
				Set<HgRoot> roots = ResourceUtils.groupByRoot(Arrays.asList(projects)).keySet();
				for (HgRoot hgRoot : roots) {
					String syncBranch = MercurialSynchronizeSubscriber.getSyncBranch(hgRoot);
					try {
						result.addAll(cache.getUnmappedChangeSets(hgRoot, repo, syncBranch, result));
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
			}
		};

		try {
			MercurialSynchronizeSubscriber.executeLockedCacheTask(runnable);
		} catch (InterruptedException e) {
			MercurialEclipsePlugin.logError(e);
			return EMPTY_SET;
		}
		return result;
	}

	public MercurialSynchronizeSubscriber getSubscriber() {
		ISynchronizationContext context = participant.getContext();
		RepositorySynchronizationScope scope = (RepositorySynchronizationScope) context.getScope();
		return scope.getSubscriber();
	}

	public RepositorySynchronizationScope getScope() {
		ISynchronizationContext context = participant.getContext();
		return (RepositorySynchronizationScope) context.getScope();
	}

	public void handleChange(IDiffChangeEvent event) {
		initializeSets();
	}

	public Set<ChangeSet> getChangeSets() {
		Set<ChangeSet> result = new HashSet<ChangeSet>();
		org.eclipse.team.internal.core.subscribers.ChangeSet[] sets = super.getSets();
		for (org.eclipse.team.internal.core.subscribers.ChangeSet set : sets) {
			result.add((ChangeSet) set);
		}
		return result;
	}

	@Override
	public void dispose() {
		getSubscriber().setCollector(null);
		MercurialTeamProvider.removeBranchListener(branchListener);
		Object[] objects = getListeners();
		for (Object object : objects) {
			removeListener((IChangeSetChangeListener) object);
		}
		super.dispose();
	}

	/**
	 * user has requested a manual refresh
	 * @param roots currently unused
	 */
	public void refresh(ResourceMapping[] roots) {
		// the line below doesn't seem to work anymore as for some reason there is no
		// diff tree events anymore if we've updated to the the changeset and it should be removed
		// fireDefaultChangedEvent(null, null);

		// TODO not sure if this is a too big hammer, but right now it seems to fix the update issue #10985
		initializeSets();
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HgChangesetsCollector [");
		if (participant != null) {
			builder.append("participant=");
			builder.append(participant);
		}
		builder.append("]");
		return builder.toString();
	}
}
