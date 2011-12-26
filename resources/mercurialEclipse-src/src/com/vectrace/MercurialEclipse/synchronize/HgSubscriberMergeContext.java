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
package com.vectrace.MercurialEclipse.synchronize;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.synchronize.cs.HgChangesetsCollector;

/**
 * @author bastian
 *
 */
public class HgSubscriberMergeContext extends SubscriberMergeContext {

	private final Set<IFile> hidden;
	private final MercurialSynchronizeSubscriber subscriber;

	public HgSubscriberMergeContext(Subscriber subscriber,
			ISynchronizationScopeManager manager) {
		super(subscriber, manager);
		initialize();
		hidden = new HashSet<IFile>();
		this.subscriber = (MercurialSynchronizeSubscriber) subscriber;
	}

	/**
	 * Called after "Overwrite" action is executed
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	protected void makeInSync(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
	}

//    @Override
//    public void run(IWorkspaceRunnable runnable, ISchedulingRule rule, int flags,
//            IProgressMonitor monitor) throws CoreException {
//        doPullAndMerge(subscriber.getRepo(), subscriber.getProjects(), runnable, rule, flags, monitor);
//    }
//
//    @Override
//    protected void runInBackground(IWorkspaceRunnable runnable) {
//        doPullAndMerge(subscriber.getRepo(), subscriber.getProjects(), runnable);
//    }

	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
	}

	public void reject(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
	}

	/**
	 * "Synchronize", part 2
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
		super.refresh(traversals, flags, monitor);
		monitor.done();
	}

	/**
	 * "Synchronize", part 1
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void refresh(ResourceMapping[] mappings, IProgressMonitor monitor) throws CoreException {
		super.refresh(mappings, monitor);
		HgChangesetsCollector collector = subscriber.getCollector();
		if(collector != null) {
			collector.refresh(mappings);
		}
	}

	@Override
	protected SyncInfo getSyncInfo(IResource resource) throws CoreException {
		return super.getSyncInfo(resource);
	}

	@Override
	public void dispose() {
		// avoid silly NPE's in the team API code if they try to dispose compare
		// editors on shutdown, we don't care
		if(!PlatformUI.getWorkbench().isClosing()) {
			super.dispose();
		}
		clearHiddenFiles();
	}

	public void hide(IFile file) {
		hidden.add(file);
//		HgChangesetsCollector collector = subscriber.getCollector();
//		if(collector != null) {
//			collector.refresh(null);
//		}
	}

	public void clearHiddenFiles(){
		hidden.clear();
	}

	public boolean isHidden(IFile file){
		return hidden.contains(file);
	}

//    private void doPullAndMerge(HgRepositoryLocation location,
//            IProject[] projects,
//            IWorkspaceRunnable runnable) {
//        doPullAndMerge(location, projects, runnable, null, 0, null);
//    }
//
//    private void doPullAndMerge(HgRepositoryLocation location,
//            IProject[] projects,
//            IWorkspaceRunnable runnable,
//            ISchedulingRule rule,
//            int flags,
//            IProgressMonitor monitor) {
//        //TODO This is a null solution to avoid wrong usage until it is properly implemented!
//        // I was originally planned to show a dialog here but that seems "impossible" :(
//    }
}
