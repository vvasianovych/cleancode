/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	        - implementation
 *     Andrei Loskutov          - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * The cache does NOT keeps the state automatically. Clients have explicitely request and manage
 * cache updates.
 * <p>
 * There is no guarantee that the data in the cache is up-to-date with the server. To get the latest
 * data, clients have explicitely refresh the cache before using it.
 * <p>
 * The cache does not maintain any states. If client "clear" this cache, it must make sure that they
 * request an explicit cache update. After "clear" and "refresh", a notification is sent to the
 * observing clients.
 * <p>
 * <b>Implementation note 1</b> the cache does not send any notifications...
 *
 * @author bastian
 * @author Andrei Loskutov
 */
public final class LocalChangesetCache extends AbstractCache {

	private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();

	private static LocalChangesetCache instance;

	/**
	 * Contains all the loaded changesets for each of the paths (resources)
	 */
	private final Map<IPath, SortedSet<ChangeSet>> localChangeSets;
	/**
	 * Stores all changesets for each project. changesets can be retreived by its rev:node or rev:shortnode strings. Only actually used for reading in
	 * getChangesetById(Project, String) which is private. This can probably be removed without adverse effect.
	 */
	private final Map<IProject, Map<String, ChangeSet>> changesets;
	/**
	 * Stores the latest changeset for each root
	 */
	private final Map<HgRoot, ChangeSet> latestChangesets;

	private int logBatchSize;

	private boolean isGetFileInformationForChangesets;

	private LocalChangesetCache() {
		super();
		localChangeSets = new HashMap<IPath, SortedSet<ChangeSet>>();
		changesets = new HashMap<IProject, Map<String, ChangeSet>>();
		latestChangesets = new HashMap<HgRoot, ChangeSet>();
	}

	private boolean isGetFileInformationForChangesets() {
		return isGetFileInformationForChangesets;
	}

	public static synchronized LocalChangesetCache getInstance() {
		if (instance == null) {
			instance = new LocalChangesetCache();
		}
		return instance;
	}

	public void clear(HgRoot root, boolean notify) {
		synchronized (latestChangesets) {
			latestChangesets.remove(root);
		}
		synchronized (localChangeSets) {
			localChangeSets.remove(root.getIPath());
		}
		Set<IProject> projects = ResourceUtils.getProjects(root);
		for (IProject project : projects) {
			clear(project, notify);
		}
	}

	/**
	 *
	 * @param resource
	 * @param notify
	 * @deprecated {@link #clear(HgRoot, boolean)} should be used in most cases
	 */
	@Deprecated
	public void clear(IResource resource, boolean notify) {
		Set<IResource> members = ResourceUtils.getMembers(resource);
		if(resource instanceof IProject && !resource.exists()) {
			members.remove(resource);
		}
		synchronized(localChangeSets){
			for (IResource member : members) {
				localChangeSets.remove(ResourceUtils.getPath(member));
			}
		}
		if(resource instanceof IProject){
			synchronized (changesets){
				changesets.remove(resource.getProject());
			}
		}
	}

	@Override
	protected void projectDeletedOrClosed(IProject project) {
		clear(project, false);
	}

	/**
	 * @param resource non null
	 * @return never null, but possibly empty set
	 */
	public SortedSet<ChangeSet> getOrFetchChangeSets(IResource resource) throws HgException {
		IPath location = ResourceUtils.getPath(resource);
		if(location.isEmpty()) {
			return EMPTY_SET;
		}

		SortedSet<ChangeSet> revisions;
		synchronized(localChangeSets){
			revisions = localChangeSets.get(location);
			if (revisions == null) {
				if (resource.getType() == IResource.FILE
						|| resource.getType() == IResource.PROJECT
						&& STATUS_CACHE.isSupervised(resource)
						&& !STATUS_CACHE.isAdded(location)) {
					refreshAllLocalRevisions(resource, true);
					revisions = localChangeSets.get(location);
				}
			}
		}
		if (revisions != null) {
			return Collections.unmodifiableSortedSet(revisions);
		}
		return EMPTY_SET;
	}

	/**
	 * @param hgRoot non null
	 * @return never null, but possibly empty set
	 */
	public SortedSet<ChangeSet> getOrFetchChangeSets(HgRoot hgRoot) throws HgException {
		IPath location = hgRoot.getIPath();

		SortedSet<ChangeSet> revisions;
		synchronized(localChangeSets){
			revisions = localChangeSets.get(location);
			if (revisions == null) {
				refreshAllLocalRevisions(hgRoot, true);
				revisions = localChangeSets.get(location);
			}
		}
		if (revisions != null) {
			return Collections.unmodifiableSortedSet(revisions);
		}
		return EMPTY_SET;
	}

	/**
	 * Gets changeset for given resource.
	 *
	 * @param resource
	 *            the resource to get status for.
	 * @return may return null
	 * @throws HgException
	 */
	public ChangeSet getNewestChangeSet(IResource resource) throws HgException {
		SortedSet<ChangeSet> revisions = getOrFetchChangeSets(resource);
		if (revisions.size() > 0) {
			return revisions.last();
		}
		return null;
	}

	public ChangeSet getNewestChangeSet(HgRoot hgRoot) throws HgException {
		SortedSet<ChangeSet> revisions = getOrFetchChangeSets(hgRoot);
		if (revisions.size() > 0) {
			return revisions.last();
		}

		return null;
	}


	/**
	 * Refreshes all local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 *
	 * If preference is set to display changeset information on label decorator,
	 * and a resource version can't be found in the topmost revisions,
	 * the last revision of this file is obtained via additional
	 * calls.
	 *
	 * @param res non null
	 * @param limit whether to limit or to have full project log
	 * @throws HgException
	 *
	 * @see #refreshAllLocalRevisions(IResource, boolean, boolean)
	 */
	public void refreshAllLocalRevisions(IResource res, boolean limit) throws HgException {
		refreshAllLocalRevisions(res, limit, isGetFileInformationForChangesets());
	}

	/**
	 * Refreshes all local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 * <p>
	 * A clear of all existing data for the given resource is triggered.
	 * <p>
	 * If withFiles is true and a resource version can't be found in the topmost
	 * revisions, the last revision of this file is obtained via additional
	 * calls.
	 *
	 * @param res non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @param withFiles
	 *            true = include file in changeset
	 * @throws HgException
	 */
	public void refreshAllLocalRevisions(IResource res, boolean limit,
			boolean withFiles) throws HgException {
		Assert.isNotNull(res);
		IProject project = res.getProject();
		if (MercurialTeamProvider.isHgTeamProviderFor(project)) {
			clear(res, false);
			int versionLimit = getLogBatchSize();
			if(withFiles && versionLimit > 1) {
				versionLimit = 1;
			}
			fetchRevisions(res, limit, versionLimit, -1, withFiles);
		}
	}

	public Set<ChangeSet> refreshAllLocalRevisions(HgRoot hgRoot, boolean limit) throws HgException {
		return refreshAllLocalRevisions(hgRoot, limit, isGetFileInformationForChangesets());
	}

	/**
	 * Refreshes all local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 * <p>
	 * A clear of all existing data for the given resource is triggered.
	 * <p>
	 * If withFiles is true and a resource version can't be found in the topmost
	 * revisions, the last revision of this file is obtained via additional
	 * calls.
	 *
	 * @param hgRoot non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @param withFiles
	 *            true = include file in changeset
	 * @throws HgException
	 */
	public Set<ChangeSet> refreshAllLocalRevisions(HgRoot hgRoot, boolean limit,
			boolean withFiles) throws HgException {
		Assert.isNotNull(hgRoot);
		clear(hgRoot, false);
		int versionLimit = getLogBatchSize();
		if(withFiles && versionLimit > 1) {
			versionLimit = 1;
		}
		return fetchRevisions(hgRoot, limit, versionLimit, -1, withFiles);
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store){
		logBatchSize = store.getInt(MercurialPreferenceConstants.LOG_BATCH_SIZE);
		if (logBatchSize < 0) {
			logBatchSize = 2000;
			MercurialEclipsePlugin.logWarning(Messages.localChangesetCache_LogLimitNotCorrectlyConfigured, null);
		}
		isGetFileInformationForChangesets = store.getBoolean(
				MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET);
	}


	/**
	 * Gets the configured log batch size.
	 */
	public int getLogBatchSize() {
		return logBatchSize;
	}

	/**
	 * Gets changeset by its identifier
	 *
	 * @param changesetId
	 *            string in format rev:nodeshort or rev:node
	 * @return may return null, if changeset is not known
	 */
	private ChangeSet getChangesetById(IProject project, String changesetId) {
		Map<String, ChangeSet> map;
		synchronized (changesets) {
			map = changesets.get(project);
		}
		if(map != null) {
			return map.get(changesetId);
		}
		return null;
	}

	public ChangeSet getOrFetchChangeSetById(HgRoot hgRoot, String nodeId) throws HgException {
		Assert.isNotNull(hgRoot);
		Assert.isNotNull(nodeId);
		SortedSet<ChangeSet> sets = getOrFetchChangeSets(hgRoot);
		for (ChangeSet changeSet : sets) {
			if(nodeId.equals(changeSet.getChangeset())
					|| nodeId.equals(changeSet.toString())
					|| nodeId.equals(changeSet.getName())){
				return changeSet;
			}
		}
		return null;
	}

	public ChangeSet getOrFetchChangeSetById(IResource res, String nodeId) throws HgException {
		Assert.isNotNull(res);
		Assert.isNotNull(nodeId);
		ChangeSet changeSet = getChangesetById(res.getProject(), nodeId);
		if (changeSet != null) {
			return changeSet;
		}
		synchronized (localChangeSets){
			changeSet = HgLogClient.getChangeset(res, nodeId, isGetFileInformationForChangesets());
			if (changeSet == null) {
				return changeSet;
			}
			// ok, the map has to  be updated with the new info
			if(!res.exists() || STATUS_CACHE.isSupervised(res)){
				// !res.exists() is the case for renamed (moved) or copied files which does not exist anymore
				HashSet<ChangeSet> set = new HashSet<ChangeSet>();
				set.add(changeSet);
				addChangesets(res.getProject(), set);
			}
		}
		return changeSet;
	}

	/**
	 * @return may return null
	 */
	public ChangeSet getChangesetByRootId(IResource res) throws HgException {
		HgRoot root = MercurialTeamProvider.getHgRoot(res);
		if(root == null) {
			return null;
		}
		return getChangesetForRoot(root);
	}

	/**
	 * @return may return null
	 */
	public ChangeSet getChangesetForRoot(HgRoot root) throws HgException {
		// for projects in the same root try to use root cache
		synchronized (latestChangesets) {
			ChangeSet changeSet = latestChangesets.get(root);
			if(changeSet != null) {
				return changeSet;
			}
			String nodeId = HgIdentClient.getCurrentChangesetId(root);
			if (!HgIdentClient.VERSION_ZERO.equals(nodeId)) {
				ChangeSet lastSet = HgLogClient.getChangeset(root, nodeId);
				if (lastSet != null) {
					latestChangesets.put(root, lastSet);
				}
				return lastSet;
			}
		}
		return null;
	}

	/**
	 * Checks if the cache contains an old changeset. If this is the case, simply removes the cached
	 * value (new value will be retrieved later)
	 *
	 * @param root
	 *            working dir
	 * @param nodeId
	 *            latest working dir (full) changeset id
	 */
	public void checkLatestChangeset(HgRoot root, String nodeId) {
		if (nodeId == null || root == null) {
			return;
		}
		if (!HgIdentClient.VERSION_ZERO.equals(nodeId)) {
			synchronized (latestChangesets) {
				ChangeSet lastSet = latestChangesets.get(root);
				if (lastSet != null && !nodeId.equals(lastSet.getChangeset())) {
					latestChangesets.remove(root);
				}
			}
		}
	}


	/**
	 * Fetches local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 *
	 * If a resource version can't be found in the topmost revisions, the last
	 * revisions of this file (10% of limit number) are obtained via additional
	 * calls.
	 *
	 * @param res non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @param limitNumber
	 *            if limit is set, how many revisions should be fetched
	 * @param startRev
	 *            the revision to start with
	 * @throws HgException
	 */
	public void fetchRevisions(IResource res, boolean limit,
			int limitNumber, int startRev, boolean withFiles) throws HgException {
		Assert.isNotNull(res);
		IProject project = res.getProject();
		if (!project.isOpen() || !STATUS_CACHE.isSupervised(res)) {
			return;
		}
		HgRoot root = MercurialTeamProvider.getHgRoot(res);
		Assert.isNotNull(root);

		Map<IPath, Set<ChangeSet>> revisions;
		// now we may change cache state, so lock
		synchronized(localChangeSets){
			if (limit) {
				revisions = HgLogClient.getProjectLog(res, limitNumber, startRev, withFiles);
			} else {
				revisions = HgLogClient.getCompleteProjectLog(res, withFiles);
			}
			if (revisions.size() == 0) {
				return;
			}

			if (res.getType() != IResource.PROJECT) {
				IPath location = ResourceUtils.getPath(res);
				if(location.isEmpty()) {
					return;
				}
				Set<ChangeSet> csets = revisions.get(location);
				if(csets != null) {
					localChangeSets.put(location, new TreeSet<ChangeSet>(csets));
				} else {
					localChangeSets.put(location, new TreeSet<ChangeSet>());
				}
			}
			for (Map.Entry<IPath, Set<ChangeSet>> mapEntry : revisions.entrySet()) {
				IPath path = mapEntry.getKey();
				Set<ChangeSet> changes = mapEntry.getValue();
				// if changes for resource not in cache, get at least 1 revision
				if (changes == null && limit && withFiles
						&& STATUS_CACHE.isSupervised(project, path)
						&& !STATUS_CACHE.isAdded(path)) {

					IResource myResource = ResourceUtils.convertRepoRelPath(root, project, root.toRelative(path.toFile()));
					if (myResource != null) {
						changes = HgLogClient.getRecentProjectLog(myResource, 1, withFiles).get(path);
					}
				}
				// add changes to cache
				addChangesToLocalCache(project, path, changes);
			}
		}
	}

	/**
	 * Fetches local revisions. If limit is set, it looks up the default
	 * number of revisions to get and fetches the topmost till limit is reached.
	 *
	 * If a resource version can't be found in the topmost revisions, the last
	 * revisions of this file (10% of limit number) are obtained via additional
	 * calls.
	 *
	 * @param hgRoot non null
	 * @param limit
	 *            whether to limit or to have full project log
	 * @param limitNumber
	 *            if limit is set, how many revisions should be fetched
	 * @param startRev
	 *            the revision to start with
	 * @throws HgException
	 */
	public Set<ChangeSet> fetchRevisions(HgRoot hgRoot, boolean limit,
			int limitNumber, int startRev, boolean withFiles) throws HgException {
		Assert.isNotNull(hgRoot);

		Map<IPath, Set<ChangeSet>> revisions;
		// now we may change cache state, so lock
		synchronized(localChangeSets){
			if (limit) {
				revisions = HgLogClient.getRootLog(hgRoot, limitNumber, startRev, withFiles);
			} else {
				revisions = HgLogClient.getCompleteRootLog(hgRoot, withFiles);
			}
			if (revisions.size() == 0) {
				return EMPTY_SET;
			}

			Set<ChangeSet> changes = revisions.get(hgRoot.getIPath());
			// XXX should we distribute/remember changesets by project?
			addChangesToLocalCache(null, hgRoot.getIPath(), changes);
			return changes;
		}
	}

	@Override
	public synchronized void addObserver(Observer o) {
		// last implementation was very inefficient: the only listener was
		// the decorator, and this one has generated NEW cache updates each time
		// he was notified about changes, so it is an endless loop.
		// So temporary do not allow to observe this cache, until the code is improved
		// has no effect
		MercurialEclipsePlugin.logError(new UnsupportedOperationException("Observer not supported: " + o));
	}

	@Override
	public synchronized void deleteObserver(Observer o) {
		// has no effect
	}

	/**
	 * Spawns an update job to notify all the clients about given resource changes
	 * @param resource non null
	 */
	@Override
	protected void notifyChanged(final IResource resource, boolean expandMembers) {
		// has no effect
		MercurialEclipsePlugin.logError(new UnsupportedOperationException("notifyChanged not supported"));
	}

	/**
	 * Spawns an update job to notify all the clients about given resource changes
	 * @param resources non null
	 */
	@Override
	protected void notifyChanged(final Set<IResource> resources, final boolean expandMembers) {
		// has no effect
		MercurialEclipsePlugin.logError(new UnsupportedOperationException("notifyChanged not supported"));
	}

	private void addChangesets(IProject project, Set<ChangeSet> changes) {
		synchronized (changesets) {
			Map<String, ChangeSet> map = changesets.get(project);
			if(map == null){
				map = new ConcurrentHashMap<String, ChangeSet>();
				changesets.put(project, map);
			}
			for (ChangeSet changeSet : changes) {
				map.put(changeSet.toString(), changeSet);
				map.put(changeSet.getChangeset(), changeSet);
				map.put(changeSet.getName(), changeSet);
			}
		}
	}

	/**
	 * @param path absolute file path
	 * @param changes may be null
	 */
	private void addChangesToLocalCache(IProject project, IPath path, Set<ChangeSet> changes) {
		if (changes != null && changes.size() > 0) {
			SortedSet<ChangeSet> existing = localChangeSets.get(path);
			if (existing == null) {
				existing = new TreeSet<ChangeSet>();
				localChangeSets.put(path, existing);
			}
			existing.addAll(changes);
			if(project != null) {
				addChangesets(project, changes);
			}
		}
	}

	public Set<ChangeSet> getOrFetchChangeSetsByBranch(HgRoot hgRoot, String branchName)
			throws HgException {

		SortedSet<ChangeSet> changes = getOrFetchChangeSets(hgRoot);
		Set<ChangeSet> branchChangeSets = new HashSet<ChangeSet>();
		for (ChangeSet changeSet : changes) {
			String changesetBranch = changeSet.getBranch();
			if (Branch.same(branchName, changesetBranch)) {
				branchChangeSets.add(changeSet);
			}
		}
		return branchChangeSets;

	}
}
