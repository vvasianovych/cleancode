/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.diff.provider.TwoWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.NullRevision;

/**
 * @author Andrei
 */
public class DelayedSyncInfo extends MercurialSyncInfo {

	private final HgRoot root;
	private final String currentBranch;
	private final IHgRepositoryLocation repo;

	private IResourceVariant delayedBase;
	private IResourceVariant delayedRemote;
	private DelayedDiff delayedDiff;

	public DelayedSyncInfo(IFile local, HgRoot root, String currentBranch, IHgRepositoryLocation repo, IResourceVariantComparator comparator, int description) {
		super(local, null, null, comparator, description);
		this.root = root;
		this.currentBranch = currentBranch;
		this.repo = repo;
		try {
			init();
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	private void initBaseAndRemote(){
		IFile file = (IFile) getLocal();
		SyncInfo syncInfo = MercurialSynchronizeSubscriber.getSyncInfo(file, root, currentBranch, repo);
		if(syncInfo != null){
			delayedBase = syncInfo.getBase();
			delayedRemote = syncInfo.getRemote();
		} else {
			delayedBase = new MercurialResourceVariant(new NullRevision(file, null));
			delayedRemote = new MercurialResourceVariant(new NullRevision(file, null));
		}
	}

	@Override
	public IResourceVariant getBase() {
		if(delayedBase == null) {
			initBaseAndRemote();
		}
		return delayedBase;
	}

	@Override
	public IResourceVariant getRemote() {
		if(delayedRemote == null) {
			initBaseAndRemote();
		}
		return delayedRemote;
	}

	// here to avoid FindBugs warnings
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public IDiff getDiff(){
		if(delayedDiff == null){
			delayedDiff = new DelayedDiff();
		}
		return delayedDiff;
	}

	// TODO right now incoming/outgoing arrows are not shown as icon overlays
	// I guess we must use ThreeWayDiff for this...
	@SuppressWarnings("restriction")
	public class DelayedDiff extends TwoWayDiff implements IResourceDiff {

		public DelayedDiff() {
			super(getLocal().getFullPath(), IDiff.CHANGE, ITwoWayDiff.CONTENT);
		}

		public IFileRevision getAfterState() {
			ResourceVariantFileRevision revision = new ResourceVariantFileRevision(getRemote());
			return revision;
		}

		public IFileRevision getBeforeState() {
			ResourceVariantFileRevision revision = new ResourceVariantFileRevision(getBase());
			return revision;
		}

		public IResource getResource() {
			return getLocal();
		}

	}
}
