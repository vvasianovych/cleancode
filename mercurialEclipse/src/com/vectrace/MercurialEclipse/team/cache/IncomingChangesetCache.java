/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch  implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * Cache for all changesets existing locally, but not present on the server
 */
public final class IncomingChangesetCache extends AbstractRemoteCache {

	private static IncomingChangesetCache instance;

	private IncomingChangesetCache() {
		super(Direction.INCOMING);
	}

	public static synchronized IncomingChangesetCache getInstance() {
		if (instance == null) {
			instance = new IncomingChangesetCache();
		}
		return instance;
	}

	/**
	 * Gets the newest incoming changeset of the default repository. In case no default
	 * repository is set, <b>all repositories</b> are considered.
	 *
	 * @param resource
	 *            the resource to get the changeset for
	 */
	public ChangeSet getNewestChangeSet(IResource resource) throws HgException {
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
		if(hgRoot == null) {
			return null;
		}
		HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
		IHgRepositoryLocation defaultRepo = repoManager.getDefaultRepoLocation(hgRoot);
		SortedSet<ChangeSet> changeSets1 = new TreeSet<ChangeSet>();
		if(defaultRepo != null) {
			ChangeSet candidate = getNewestChangeSet(resource, defaultRepo, null);
			if (candidate != null) {
				changeSets1.add(candidate);
			}
		} else {
			Set<IHgRepositoryLocation> locs = repoManager.getAllRepoLocations(hgRoot);
			for (IHgRepositoryLocation repository : locs) {
				ChangeSet candidate = getNewestChangeSet(resource, repository, null);
				if (candidate != null) {
					changeSets1.add(candidate);
				}
			}
		}
		if (changeSets1.size() > 0) {
			return changeSets1.last();
		}
		return null;
	}
}
