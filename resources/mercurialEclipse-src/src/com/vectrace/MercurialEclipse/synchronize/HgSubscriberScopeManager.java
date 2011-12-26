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
package com.vectrace.MercurialEclipse.synchronize;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.ui.IPropertyListener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * @author Andrei
 */
public class HgSubscriberScopeManager extends SubscriberScopeManager implements Observer {

	public static final int INCOMING = -1;
	public static final int OUTGOING = -2;
	public static final int LOCAL = -3;
	private final IPropertyListener branchListener;
	private boolean disposed;

	public HgSubscriberScopeManager(ResourceMapping[] inputMappings, MercurialSynchronizeSubscriber subscriber) {
		super(HgSubscriberScopeManager.class.getSimpleName(), inputMappings, subscriber, false);

		MercurialStatusCache.getInstance().addObserver(this);
		IncomingChangesetCache.getInstance().addObserver(this);
		OutgoingChangesetCache.getInstance().addObserver(this);
		branchListener = new IPropertyListener() {
			public void propertyChanged(Object source, int propId) {
				MercurialSynchronizeSubscriber subscriber1 = (MercurialSynchronizeSubscriber) getSubscriber();
				subscriber1.branchChanged((HgRoot) source);
			}
		};
		MercurialTeamProvider.addBranchListener(branchListener);
	}

	@Override
	public ISynchronizationScope getScope() {
		// Does NOT return expected (our) scope, but ResourceMappingScope...
		// super.getScope();
		RepositorySynchronizationScope scope2 = ((MercurialSynchronizeSubscriber) getSubscriber()).getScope();
		return scope2;
	}

	@Override
	public void initialize(IProgressMonitor monitor) throws CoreException {
		super.initialize(monitor);
//		MercurialSynchronizeParticipant participant = ((MercurialSynchronizeSubscriber) getSubscriber()).getParticipant();
//		HgSubscriberMergeContext context2 = (HgSubscriberMergeContext) participant.getContext();
//		context2.clearHiddenFiles();
	}

	public void update(Observable o, Object arg) {
		if(!(arg instanceof Set<?>)){
			return;
		}
		Set<ISubscriberChangeEvent> changeEvents = new HashSet<ISubscriberChangeEvent>();
		Set<?> resources = (Set<?>) arg;
		IResource[] roots = getSubscriber().roots();
		boolean projectRefresh = false;
		int flags = ISubscriberChangeEvent.SYNC_CHANGED;
		for (Object res : resources) {
			if(!(res instanceof IResource)) {
				continue;
			}
			IResource resource = (IResource) res;
			for (IResource root : roots) {
				if(root.contains(resource)) {
					if(!projectRefresh && resource.contains(root)){
						projectRefresh = true;
					}
					changeEvents.add(new HgSubscriberChangeEvent(getSubscriber(), flags, resource));
					break;
				}
			}
		}

		if (changeEvents.size() == 0) {
			return;
		}



		// XXX check the usage of the arguments set. Now it may contain more then one project
		// in the set. Hovewer, we may refresh too much here...
		if(projectRefresh && (resources.size() == 1 || containsOnlyProjects(resources))){
			// we must sync the data for the project

			if(MercurialEclipsePlugin.getDefault().isDebugging()) {
				System.out.println("! Update data from: " + o + " : " + resources.size());
			}

			int flag = 0;
			if (o instanceof IncomingChangesetCache) {
				flag = INCOMING;
			} else if (o instanceof OutgoingChangesetCache || o instanceof LocalChangesetCache) {
				flag = OUTGOING;
			} else if (o instanceof MercurialStatusCache) {
				flag = LOCAL;
			}
			updateData(roots, flag);
		} else {
			// we must update our sync UI with new data we already got

			if(MercurialEclipsePlugin.getDefault().isDebugging()) {
				System.out.println("Update UI from: " + o + " : " + resources.size());
			}

			updateUI(changeEvents);
		}
	}

	private boolean containsOnlyProjects(Set<?> resources) {
		for (Object object : resources) {
			if(!(object instanceof IProject)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void dispose() {
		if(disposed){
			return;
		}
		disposed = true;
		MercurialStatusCache.getInstance().deleteObserver(this);
		IncomingChangesetCache.getInstance().deleteObserver(this);
		OutgoingChangesetCache.getInstance().deleteObserver(this);
		MercurialTeamProvider.removeBranchListener(branchListener);
		super.dispose();
	}

	private void updateUI(Set<ISubscriberChangeEvent> events) {
		UpdateUIJob updateJob = new UpdateUIJob(events, (MercurialSynchronizeSubscriber) getSubscriber());
		Job[] jobs = Job.getJobManager().find(UpdateUIJob.class);
		for (Job job : jobs) {
			if(updateJob.equals(job)){
				job.cancel();
				if(MercurialEclipsePlugin.getDefault().isDebugging()) {
					System.out.println("Cancelled refresh UI: " + ((UpdateUIJob) job).events.size());
				}
			}
		}
		updateJob.schedule(500);
	}

	private void updateData(IResource[] roots, int flag) {
		UpdateDataJob updateJob = new UpdateDataJob(flag, roots, (MercurialSynchronizeSubscriber) getSubscriber());
		Job[] jobs = Job.getJobManager().find(UpdateDataJob.class);
		for (Job job : jobs) {
			if(updateJob.equals(job)){
				job.cancel();
				if(MercurialEclipsePlugin.getDefault().isDebugging()) {
					System.out.println("Cancelled refresh data: " + flag);
				}
			}
		}
		updateJob.schedule(200);
	}

	private static class UpdateUIJob extends Job {

		private final MercurialSynchronizeSubscriber subscriber;
		private final Set<ISubscriberChangeEvent> events;

		public UpdateUIJob(Set<ISubscriberChangeEvent> events, MercurialSynchronizeSubscriber subscriber) {
			super("Hg subscriber UI update");
			this.events = events;
			this.subscriber = subscriber;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingResources"), 1);
			try {
				ISubscriberChangeEvent[] deltas = events.toArray(new ISubscriberChangeEvent[events.size()]);
				subscriber.fireTeamResourceChange(deltas);
				monitor.worked(1);
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return UpdateUIJob.class.equals(family);
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof UpdateUIJob)){
				return false;
			}
			UpdateUIJob job = (UpdateUIJob) obj;
			if(events.size() != job.events.size()){
				return false;
			}

			if(!subscriber.equals(job.subscriber)){
				return false;
			}
			return events.containsAll(job.events);
		}

		@Override
		public int hashCode() {
			return events.size() + subscriber.hashCode();
		}
	}

	private static class UpdateDataJob extends Job {

		private final int flag;
		private final IResource[] roots;
		private final MercurialSynchronizeSubscriber subscriber;

		public UpdateDataJob(int flag, IResource[] roots, MercurialSynchronizeSubscriber subscriber) {
			super("Hg subscriber data update");
			this.flag = flag;
			this.roots = roots;
			this.subscriber = subscriber;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingResources"), 5);
			try {
				subscriber.refresh(roots, flag, monitor);
			} catch (TeamException e) {
				MercurialEclipsePlugin.logError(e);
				return Status.CANCEL_STATUS;
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return UpdateDataJob.class.equals(family);
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof UpdateDataJob)){
				return false;
			}
			UpdateDataJob job = (UpdateDataJob) obj;
			if(flag != job.flag){
				return false;
			}
			return subscriber.equals(job.subscriber);
		}

		@Override
		public int hashCode() {
			return flag + subscriber.hashCode();
		}
	}
}
