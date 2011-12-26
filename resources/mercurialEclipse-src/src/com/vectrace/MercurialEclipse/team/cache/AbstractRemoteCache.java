/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei Loskutov         - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIncomingClient;
import com.vectrace.MercurialEclipse.commands.HgOutgoingClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * A base class for remote caches (caching changesets which are either not present
 * locally but existing on the server, or existing locally, but not present on the server).
 * <p>
 * The cache keeps the state automatically (and fetches the data on client request only), to avoid
 * unneeded client-server communication.
 * <p>
 * There is no guarantee that the data in the cache is up-to-date with the server. To get the
 * latest data, clients have explicitely refresh or clean the cache before using it.
 * <p>
 * The cache has empty ("invalid" state) before the first client request and automatically
 * retrieves the data on first client request. So it becames "valid" state and does not refresh the
 * data until some operation "clears" or explicitely requests a "refresh" of the cache. After the
 * "clear" operation the cache is going to the initial "invalid" state again. After "clear" and
 * "refresh", a notification is sent to the observing clients.
 * <p>
 * <b>Implementation note 1</b> this cache <b>automatically</b> keeps the "valid" state for given
 * project/repository pair. Before each "get" request the cache validates itself. If the cached
 * value is NULL, then the cache state is invalid, and new data is fetched. If the cached value is
 * an object (even empty set), then the cache is "valid" (there is simply no data on the server).
 * <p>
 * <b>Implementation note 2</b> the cache sends different notifications depending on what kind of
 * "state change" happened. After "clear", a set with only one "project" object is sent. After
 * "refresh", a set with all changed elements is sent, which may also include a project.
 *
 * @author bastian
 * @author Andrei Loskutov
 * @author <a href="mailto:adam.berkes@intland.com">Adam Berkes</a>
 */
public abstract class AbstractRemoteCache extends AbstractCache {

	/**
	 * Map hg root -> branch -> repo -> projects -> changeset
	 */
	protected final Map<HgRoot, Set<RemoteData>> repoDatas;
	protected final Map<RemoteKey, RemoteData> fastRepoMap;

	protected final Direction direction;

	/**
	 * @param direction non null
	 */
	public AbstractRemoteCache(Direction direction) {
		this.direction = direction;
		repoDatas = new HashMap<HgRoot, Set<RemoteData>>();
		fastRepoMap = new HashMap<RemoteKey, RemoteData>();
	}

	/**
	 * does nothing, clients has to override and update preferences
	 */
	@Override
	protected void configureFromPreferences(IPreferenceStore store) {
		// does nothing
	}

	public void clear(IHgRepositoryLocation repo) {
		synchronized (repoDatas) {
			Collection<Set<RemoteData>> values = repoDatas.values();
			for (Set<RemoteData> set : values) {
				Iterator<RemoteData> iterator = set.iterator();
				while (iterator.hasNext()) {
					RemoteData data = iterator.next();
					if(repo.equals(data.getRepo())){
						iterator.remove();
						fastRepoMap.remove(data.getKey());
					}
				}
			}
			notifyChanged(repo, false);
		}
	}

	public void clear(HgRoot root, boolean notify) {
		synchronized (repoDatas) {
			Set<RemoteData> set = repoDatas.get(root);
			if(set == null){
				return;
			}
			for (RemoteData data : set) {
				fastRepoMap.remove(data.getKey());
			}
			set.clear();
		}
		if(notify) {
			notifyChanged(root, false);
		}
	}

	@SuppressWarnings("unchecked")
	private void notifyChanged(HgRoot root, boolean expandMembers) {
		Set<?> projects = ResourceUtils.getProjects(root);
		notifyChanged((Set<IResource>) projects, expandMembers);
	}

	/**
	 * @param notify true to send a notification if the cache state changes after this operation,
	 * false to supress the event notification
	 */
	public void clear(IHgRepositoryLocation repo, IProject project, boolean notify) {
		synchronized (repoDatas) {
			HgRoot hgRoot = MercurialTeamProvider.getHgRoot(project);
			Set<RemoteData> set = repoDatas.get(hgRoot);
			if(set == null){
				return;
			}
			Iterator<RemoteData> iterator = set.iterator();
			while(iterator.hasNext()) {
				RemoteData data = iterator.next();
				if(repo.equals(data.getRepo())){
					iterator.remove();
					fastRepoMap.remove(data.getKey());
				}
			}
		}
		if(notify) {
			notifyChanged(repo, false);
		}
	}

	@SuppressWarnings("unchecked")
	protected void notifyChanged(IHgRepositoryLocation repo, boolean expandMembers){
		Set<?> projects = MercurialEclipsePlugin.getRepoManager().getAllRepoLocationProjects(repo);
		notifyChanged((Set<IResource>) projects, expandMembers);
	}

	@Override
	protected void projectDeletedOrClosed(IProject project) {
		synchronized (repoDatas) {
			for (RemoteData data : fastRepoMap.values()) {
				data.clear(project);
			}
		}
	}

	/**
	 * Gets all (in or out) changesets of the given location for the given
	 * IResource.
	 *
	 * @param branch name of branch (default or "" for unnamed) or null if branch unaware
	 * @return never null
	 */
	public SortedSet<ChangeSet> getChangeSets(IResource resource,
			IHgRepositoryLocation repository, String branch) throws HgException {
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
		if (hgRoot == null){
			// if mercurial is not team provider or if we're working on a closed project
			return EMPTY_SET;
		}
		RemoteKey key = new RemoteKey(hgRoot, repository, branch);
		synchronized (repoDatas){
			RemoteData data = fastRepoMap.get(key);
			if(data == null){
				// lazy loading: refresh cache on demand only.
				// lock the cache till update is complete
				addResourcesToCache(key);
				// XXX not sure if the full repo refresh event need to be sent here
//				notifyChanged(key.getRepo(), true);
				notifyChanged(hgRoot, true);
			}
			RemoteData remoteData = fastRepoMap.get(key);
			if(remoteData != null) {
				return remoteData.getChangeSets(resource);
			}
		}
		return EMPTY_SET;
	}

	/**
	 * Gets all (in or out) changesets of the given location for the given
	 * IResource.
	 *
	 * @param branch name of branch (default or "" for unnamed) or null if branch unaware
	 * @return never null
	 */
	public SortedSet<ChangeSet> hasChangeSets(IResource resource, IHgRepositoryLocation repository,
			String branch) {
		// also checks if mercurial is team provider and if we're working on an open project
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
		if (hgRoot == null){
			// if mercurial is not team provider or if we're working on a closed project
			return EMPTY_SET;
		}
		RemoteKey key = new RemoteKey(hgRoot, repository, branch);
		synchronized (repoDatas){
			RemoteData data = fastRepoMap.get(key);

			if(data == null){
				return EMPTY_SET;
			}
			return data.getChangeSets(resource);
		}
	}

	/**
	 * Gets all (in or out) changesets of the given hg root
	 *
	 * @param branch name of branch (default or "" for unnamed) or null if branch unaware
	 * @return never null
	 */
	public SortedSet<ChangeSet> getChangeSets(HgRoot hgRoot,
			IHgRepositoryLocation repository, String branch) throws HgException {
		return getChangeSets(hgRoot, repository, branch, false);
	}

	/**
	 * Gets all (in or out) changesets of the given hg root
	 *
	 * @param branch name of branch (default or "" for unnamed) or null if branch unaware
	 * @param allowUnrelated True if unrelated repositories are acceptable
	 * @return never null
	 */
	public SortedSet<ChangeSet> getChangeSets(HgRoot hgRoot, IHgRepositoryLocation repository,
			String branch, boolean allowUnrelated) throws HgException {
		RemoteKey key = new RemoteKey(hgRoot, repository, branch, allowUnrelated);
		synchronized (repoDatas){
			RemoteData data = fastRepoMap.get(key);
			if(data == null){
				// lazy loading: refresh cache on demand only.
				// lock the cache till update is complete
				addResourcesToCache(key);
				notifyChanged(hgRoot, true);
			}
			RemoteData remoteData = fastRepoMap.get(key);
			if(remoteData != null) {
				return remoteData.getChangeSets();
			}
		}
		return EMPTY_SET;
	}

	/**
	 * Gets all (in or out) changesets for given hg root, which doesn't have any relationship to the
	 * projects inside Eclipse workspace (e.g. changesets with no files or with files which are
	 * unknown in terms of Eclipse workspace). Specifying an optional 'canIgnore' argument
	 * may help to optimize the work on huge amount of changesets or files inside.
	 *
	 * @param canIgnore
	 *            (may be null) changesets which are already known to be mapped and can be ignored.
	 *
	 * @param branch
	 *            name of branch (default or "" for unnamed) or null if branch unaware
	 * @return never null
	 */
	public SortedSet<ChangeSet> getUnmappedChangeSets(HgRoot hgRoot,
			IHgRepositoryLocation repository, String branch, Set<ChangeSet> canIgnore) throws HgException {

		SortedSet<ChangeSet> all = getChangeSets(hgRoot, repository, branch);
		if(all.isEmpty()){
			return all;
		}
		if(canIgnore != null && !canIgnore.isEmpty()) {
			// 'all' was unmodifiable set, so create a copy here for filtering
			all = new TreeSet<ChangeSet>(all);
			all.removeAll(canIgnore);
			if(all.isEmpty()){
				return all;
			}
		}
		TreeSet<ChangeSet> sorted = new TreeSet<ChangeSet>();
		for (ChangeSet cs : all) {
			if(cs.isEmpty()){
				sorted.add(cs);
				continue;
			}
			Set<IFile> files = cs.getFiles();
			if(files.isEmpty()){
				sorted.add(cs);
			}
		}
		return sorted;
	}

	/**
	 * Gets all resources that are changed in (in or out) changesets of given
	 * repository, even resources not known in local workspace.
	 *
	 * @return never null
	 */
	public Set<IResource> getMembers(IResource resource,
			IHgRepositoryLocation repository, String branch) throws HgException {
		SortedSet<ChangeSet> changeSets;
		synchronized (repoDatas){
			// make sure data is there: will refresh (in or out) changesets if needed
			changeSets = getChangeSets(resource, repository, branch);
			return getMembers(resource, changeSets);
		}
	}

	/**
	 * @return never null
	 */
	private static Set<IResource> getMembers(IResource resource,
			SortedSet<ChangeSet> changeSets) {
		Set<IResource> members = new HashSet<IResource>();
		if (changeSets == null) {
			return members;
		}
		for (ChangeSet cs : changeSets) {
			members.addAll(cs.getFiles());
		}
		return members;
	}

	private void addResourcesToCache(RemoteKey key) throws HgException {
		if(debug) {
			System.out.println("!fetch " + direction + " for " + key);
		}

		fastRepoMap.remove(key);

		// get changesets from hg
		RemoteData data = null;
		if (direction == Direction.OUTGOING) {
			data = HgOutgoingClient.getOutgoing(key);
		} else {
			data = HgIncomingClient.getHgIncoming(key);
		}

		if(debug) {
			System.out.println("!got " + data.getChangeSets().size() + " " + direction + " changesets");
		}
		fastRepoMap.put(key, data);

		Set<RemoteData> set = repoDatas.get(key.getRoot());
		if(set == null){
			set = new HashSet<RemoteData>();
			repoDatas.put(key.getRoot(), set);
		}
		set.add(data);
	}

	/**
	 * Get newest revision of resource on given branch
	 * @param resource Eclipse resource (e.g. a file) to find latest changeset for
	 * @param branch name of branch (default or "" for unnamed) or null if branch unaware
	 */
	public ChangeSet getNewestChangeSet(IResource resource,
			IHgRepositoryLocation repository, String branch) throws HgException {

		if (MercurialStatusCache.getInstance().isSupervised(resource) || !resource.exists()) {
			synchronized (repoDatas){
				// make sure data is there: will refresh (in or out) changesets if needed
				SortedSet<ChangeSet> changeSets = getChangeSets(resource, repository, branch);

				if (changeSets != null && changeSets.size() > 0) {
					return changeSets.last();
				}
			}
		}
		return null;
	}
}
