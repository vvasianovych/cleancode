/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - large contribution
 *     Jerome Negre              - fixing folders' state
 *     Bastian Doetsch	         - extraction from DecoratorStatus + additional methods
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.Team;

import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgSubreposClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.Bits;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Caches the Mercurial Status of each file and offers methods for retrieving, clearing and
 * refreshing repository state.
 *
 * @author Bastian Doetsch
 */
public final class MercurialStatusCache extends AbstractCache implements IResourceChangeListener {

	private static final int STATUS_BATCH_SIZE = 10;
	static final int NUM_CHANGED_FOR_COMPLETE_STATUS = 50;

	/**
	 * @author Andrei
	 */
	private final class ProjectUpdateJob extends Job {

		private final IProject project;
		private final RootResourceSet<IResource> resources;

		private ProjectUpdateJob(RootResourceSet<IResource> removedSet, RootResourceSet<IResource> changedSet,
				IProject project, RootResourceSet<IResource> addedSet) {
			super(Messages.mercurialStatusCache_RefreshStatus);
			this.project = project;
			resources = new RootResourceSet<IResource>();

			if(removedSet != null) {
				resources.addAll(removedSet);
			}
			if(changedSet != null) {
				resources.addAll(changedSet);
			}
			if(addedSet != null) {
				resources.addAll(addedSet);
			}

			if(resources.contains(project) || resources.size() > NUM_CHANGED_FOR_COMPLETE_STATUS){
				// refreshing the status of too many files, just refresh the whole project
				HgRoot projectRoot = resources.rootOf(project);
				if(projectRoot == null){
					projectRoot = MercurialTeamProvider.getHgRoot(project);
				}
				if(projectRoot != null) {
					resources.clear();
					resources.add(projectRoot, project);
				}
			}
		}


		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// now process gathered changes (they are in the lists)
			if(monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			try {
				updateProject(monitor);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				return e.getStatus();
			}  finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		private void updateProject(IProgressMonitor monitor) throws HgException {
			if (resources.size() == 1 && resources.contains(project)) {
				monitor.beginTask(NLS.bind(Messages.mercurialStatusCache_RefreshingProject, project.getName()), 1);
				// do not need to call notifyChanged(resources): refreshStatus() does it already
				refreshStatus(project, monitor);
			} else if(!resources.isEmpty()) {
				monitor.beginTask(Messages.mercurialStatusCache_RefreshingResources, 1);
				// do not need to call notifyChanged(resources): refreshStatus() does it already
				refreshStatus(resources, project);
			}
			monitor.worked(1);
		}

		@Override
		public boolean belongsTo(Object family) {
			return ProjectUpdateJob.class.equals(family);
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof ProjectUpdateJob)){
				return false;
			}
			ProjectUpdateJob job = (ProjectUpdateJob) obj;
			if(resources.size() != job.resources.size()){
				return false;
			}
			if(!project.equals(job.project)){
				return false;
			}
			return resources.equals(job.resources);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	private final class MemberStatusVisitor {

		int bitSet;

		public MemberStatusVisitor(IPath parentLocation, int bitSet) {
			this.bitSet = bitSet;
		}

		public boolean visit(IPath childLocation) {
			Integer memberBitSet = statusMap.get(childLocation);
			if (memberBitSet != null) {
				if(Bits.contains(memberBitSet.intValue(), MODIFIED_MASK)){
					bitSet |= BIT_MODIFIED;
					// now we are dirty, so we can stop
					return false;
				}
			}
			return true;
		}

	}

	/**
	 * Initialization On Demand Holder idiom, thread-safe and instance will not be created until getInstance is called
	 * in the outer class.
	 */
	private static final class MercurialStatusCacheHolder {
		private MercurialStatusCacheHolder() { /* hide constructor of utility class. */ }
		public static final MercurialStatusCache INSTANCE = new MercurialStatusCache();
	}

	public static final int BIT_IGNORE = 1 << 1;
	public static final int BIT_CLEAN = 1 << 2;
	/** file is tracked by hg, but it is missing on a disk (probably deleted by external command) */
	public static final int BIT_MISSING = 1 << 3;
	public static final int BIT_REMOVED = 1 << 4;
	public static final int BIT_UNKNOWN = 1 << 5;
	public static final int BIT_ADDED = 1 << 6;
	public static final int BIT_MODIFIED = 1 << 7;
	public static final int BIT_IMPOSSIBLE = 1 << 8;
	public static final int BIT_CONFLICT = 1 << 9;
	/** directory bit */
	public static final int BIT_DIR = 1 << 10;

	private static final Integer IGNORE = Integer.valueOf(BIT_IGNORE);
	private static final Integer CLEAN = Integer.valueOf(BIT_CLEAN);
//    private final static Integer _MISSING = Integer.valueOf(BIT_MISSING);
//    private final static Integer _REMOVED = Integer.valueOf(BIT_REMOVED);
//    private final static Integer _UNKNOWN = Integer.valueOf(BIT_UNKNOWN);
//    private final static Integer _ADDED = Integer.valueOf(BIT_ADDED);
//    private final static Integer _MODIFIED = Integer.valueOf(BIT_MODIFIED);
//    private final static Integer _IMPOSSIBLE = Integer.valueOf(BIT_IMPOSSIBLE);
	private static final Integer CONFLICT = Integer.valueOf(BIT_CONFLICT);

	/** maximum bits count used in the cache */
//    private final static int MAX_BITS_COUNT = 9;

	public static final char CHAR_IGNORED = 'I';
	public static final char CHAR_CLEAN = 'C';
	public static final char CHAR_MISSING = '!';
	public static final char CHAR_REMOVED = 'R';
	public static final char CHAR_UNKNOWN = '?';
	public static final char CHAR_ADDED = 'A';
	public static final char CHAR_MODIFIED = 'M';
	public static final char CHAR_UNRESOLVED = 'U';
	public static final char CHAR_RESOLVED = 'R';

	/**
	 * If the child file has any of the bits set: BIT_IGNORE | BIT_CLEAN |
	 *  BIT_MISSING | BIT_REMOVED | BIT_UNKNOWN | BIT_ADDED,
	 * we do not propagate this bits to the parent directory directly,
	 * but propagate only {@link #BIT_MODIFIED} or {@link #BIT_CONFLICT}
	 */
	private static final int IGNORED_MASK = BIT_IGNORE | BIT_CLEAN | BIT_MISSING | BIT_REMOVED
			| BIT_UNKNOWN | BIT_ADDED;

	/**
	 * We propagate only {@link #BIT_MODIFIED} bit to the parent directory, if any from bits:
	 * BIT_MISSING | BIT_REMOVED | BIT_UNKNOWN | BIT_ADDED | BIT_MODIFIED is set on the child file.
	 */
	public static final int MODIFIED_MASK = BIT_MISSING | BIT_REMOVED | BIT_UNKNOWN | BIT_ADDED
			| BIT_MODIFIED;

	/** a directory is still supervised if one of the following bits is set */
	private static final int DIR_SUPERVISED_MASK = BIT_ADDED | BIT_CLEAN | BIT_MISSING
		| BIT_MODIFIED | BIT_REMOVED | BIT_CONFLICT;

	/**  an "added" directory is only added if NONE of the following bits is set */
	private static final int DIR_NOT_ADDED_MASK = BIT_CLEAN | BIT_MISSING
		| BIT_MODIFIED | BIT_REMOVED | BIT_CONFLICT | BIT_IGNORE;


	protected static final int MASK_CHANGED = IResourceDelta.OPEN | IResourceDelta.CONTENT
		| IResourceDelta.MOVED_FROM | IResourceDelta.REPLACED | IResourceDelta.TYPE;

	protected static final int MASK_DELTA = MASK_CHANGED | IResourceDelta.MOVED_TO
		| IResourceDelta.ADDED | IResourceDelta.COPIED_FROM | IResourceDelta.REMOVED;

	/** Used to store the last known status of a resource */
	/* private */final ConcurrentHashMap<IPath, Integer> statusMap = new ConcurrentHashMap<IPath, Integer>(
			10000, 0.75f, 4);
	private final BitMap bitMap;
	private final Object statusUpdateLock = new byte[0];

	/** Used to store which projects have already been parsed */
	private final CopyOnWriteArraySet<IProject> knownStatus = new CopyOnWriteArraySet<IProject>();

	private final ConcurrentHashMap<IPath, String> mergeChangesetIds = new ConcurrentHashMap<IPath, String>(
			100, 0.75f, 4);

	private int statusBatchSize;
	private boolean enableSubrepos;

	static class BitMap {
		private final PathsSet ignore = new PathsSet(1000, 0.75f);
		// don't waste space with most popular state
		// private final Set<IPath> clean = new HashSet<IPath>();
		private final PathsSet missing = new PathsSet(1000, 0.75f);
		private final PathsSet removed = new PathsSet(1000, 0.75f);
		private final PathsSet unknown = new PathsSet(1000, 0.75f);
		private final PathsSet added = new PathsSet(1000, 0.75f);
		private final PathsSet modified = new PathsSet(1000, 0.75f);
		private final PathsSet conflict = new PathsSet(1000, 0.75f);
		/** directories */
		private final PathsSet dir = new PathsSet(1000, 0.75f);
		// we do not cache impossible values
		// private final Set<IPath> impossible = new HashSet<IPath>();

		public BitMap() {
			super();
		}

		synchronized void put(IPath path, Integer set){
			// removed is the first one for speed
			int mask = set.intValue();
			if((mask & BIT_REMOVED) != 0){
				removed.add(path);
			}
			if((mask & BIT_MISSING) != 0){
				missing.add(path);
			}
			if((mask & BIT_UNKNOWN) != 0){
				unknown.add(path);
			}
			if((mask & BIT_ADDED) != 0){
				added.add(path);
			}
			if((mask & BIT_MODIFIED) != 0){
				modified.add(path);
			}
			if((mask & BIT_CONFLICT) != 0){
				conflict.add(path);
			}
			if((mask & BIT_IGNORE) != 0){
				ignore.add(path);
			}
			if((mask & BIT_DIR) != 0){
				dir.add(path);
			}
		}

		synchronized PathsSet get(int bit){
			switch (bit) {
			case BIT_REMOVED:
				return removed;
			case BIT_MISSING:
				return missing;
			case BIT_UNKNOWN:
				return unknown;
			case BIT_ADDED:
				return added;
			case BIT_MODIFIED:
				return modified;
			case BIT_CONFLICT:
				return conflict;
			case BIT_IGNORE:
				return ignore;
			case BIT_DIR:
				return dir;
			default:
				return null;
			}
		}

		synchronized void remove(IPath path) {
			remove(path, removed);
			remove(path, missing);
			remove(path, unknown);
			remove(path, added);
			remove(path, modified);
			remove(path, conflict);
			remove(path, ignore);
			remove(path, dir);
		}

		static void remove(IPath path, PathsSet set) {
			if(!set.isEmpty()) {
				set.remove(path);
			}
		}
	}

	private MercurialStatusCache() {
		super();
		bitMap = new BitMap();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
	}

	public static MercurialStatusCache getInstance() {
		return MercurialStatusCacheHolder.INSTANCE;
	}

	/**
	 * Checks if status for given project is known.
	 *
	 * @param project
	 *            the project to be checked
	 * @return true if known, false if not.
	 */
	public boolean isStatusKnown(IProject project) {
		return project != null && knownStatus.contains(project);
	}

	/**
	 * Gets the status of the given resource from cache. The returned BitSet contains a BitSet of the status flags set.
	 *
	 * The flags correspond to the BIT_* constants in this class.
	 *
	 * @param resource
	 *            the resource to get status for.
	 * @return the BitSet with status flags, MAY RETURN NULL, if status is unknown yet
	 */
	public Integer getStatus(IResource resource) {
		IPath location = resource.getLocation();
		return getStatus(location);
	}


	/**
	 * Gets the status of the given path from cache. The returned BitSet contains a BitSet of the status flags set.
	 *
	 * The flags correspond to the BIT_* constants in this class.
	 *
	 * @param location
	 *            the absolute file system path to get status for (can be null).
	 * @return the BitSet with status flags, MAY RETURN NULL, if status is unknown yet
	 */
	private Integer getStatus(IPath location) {
		return location != null? statusMap.get(location) : null;
	}

	public boolean isSupervised(IResource resource) {
		return isSupervised(resource, resource.getLocation());
	}

	public boolean isSupervised(IResource resource, IPath path) {
		if(path == null){
			return false;
		}
		Integer statusInt = statusMap.get(path);
		if(statusInt == null){
			return false;
		}
		Assert.isNotNull(resource);
		IProject project = resource.getProject();
		if (path.equals(project.getLocation())) {
			return MercurialTeamProvider.isHgTeamProviderFor(project);
		}
		int status = statusInt.intValue();
		int highestBit = Bits.highestBit(status);
		switch (highestBit) {
		case BIT_IGNORE:
		case BIT_UNKNOWN:
			if (resource.getType() != IResource.FILE && highestBit != BIT_IGNORE) {
				// check for Eclipse ignore settings
				if (Team.isIgnoredHint(resource)) {
					return false;
				}
				// a directory is still supervised if one of the lower bits set
				return Bits.contains(status, DIR_SUPERVISED_MASK);
			}
			return false;
		}
		return true;

	}

	public boolean isAdded(IPath path) {
		Assert.isNotNull(path);
		Integer statusInt = statusMap.get(path);
		if(statusInt == null){
			return false;
		}
		int status = statusInt.intValue();
		if (Bits.highestBit(status) == BIT_ADDED) {
			File fileSystemResource = path.toFile();
			if (fileSystemResource.isDirectory()) {
				return Bits.contains(status, DIR_NOT_ADDED_MASK);
			}
			return true;
		}
		return false;
	}

	public boolean isDirectory(IPath location) {
		if(location == null){
			return false;
		}
		return bitMap.get(BIT_DIR).contains(location);
	}

	public boolean isRemoved(IResource resource) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null){
			return false;
		}
		return Bits.contains(status.intValue(), BIT_REMOVED);
	}

	public boolean isUnknown(IResource resource) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null){
			// since we track everything now, all "unknown" files are really unknown
			return true;
		}
		return Bits.contains(status.intValue(), BIT_UNKNOWN);
	}

	public boolean isIgnored(IResource resource) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null) {
			if (isStatusKnown(resource.getProject())) {
				// it seems that original autors intentionally do not tracked status for
				// ignored files. I guess the reason was performance: for a java project,
				// including "ignored" class files would double the cache size...
				return true;
			}
			return false;
		}

		return Bits.contains(status.intValue(), BIT_IGNORE);
	}

	/**
	 * @param location
	 *            the absolute file system path to get status for (can be null).
	 * @return true if the cache knows that the given path should be ignored by Mercurial
	 */
	public boolean isIgnored(IPath location) {
		Integer status = getStatus(location);
		if(status == null) {
			return false;
		}
		return Bits.contains(status.intValue(), BIT_IGNORE);
	}

	/**
	 * @see #BIT_CLEAN
	 */
	public boolean isClean(IResource resource) {
		return isStatus(resource, BIT_CLEAN);
	}

	/**
	 * @see #BIT_CONFLICT
	 */
	public boolean isConflict(IResource resource) {
		return isStatus(resource, BIT_CONFLICT);
	}

	private boolean isStatus(IResource resource, int flag) {
		Assert.isNotNull(resource);
		Integer status = getStatus(resource);
		if(status == null){
			return false;
		}
		return Bits.contains(status.intValue(), flag);
	}

	/**
	 *
	 * @param statusBit
	 * @param parent
	 * @return may return null, if no paths for given parent and bitset are known
	 */
	private List<IPath> getPaths(int statusBit, IPath parent){
		boolean isMappedState = statusBit != BIT_CLEAN && statusBit != BIT_IMPOSSIBLE;
		if(!isMappedState) {
			return null;
		}
		PathsSet all = bitMap.get(statusBit);
		if(all.isEmpty()){
			return null;
		}
		return all.getChildren(parent);
	}

	/**
	 *
	 * @param statusBit
	 * @param parent
	 * @return may return null, if no paths for given parent and bitset are known
	 */
	private List<IPath> getDirectChildren(int statusBit, IPath parent){
		boolean isMappedState = statusBit != BIT_CLEAN && statusBit != BIT_IMPOSSIBLE;
		if(!isMappedState) {
			return null;
		}
		PathsSet all = bitMap.get(statusBit);
		if(all.isEmpty()){
			return null;
		}
		return all.getDirectChildren(parent);
	}

	public Set<IFile> getFiles(int statusBits, IContainer folder){
		Set<IResource> resources = getResources(statusBits, folder);
		Set<IFile> files = new HashSet<IFile>();
		for (IResource resource : resources) {
			IPath location = resource.getLocation();
			if(resource instanceof IFile && location != null && !location.toFile().isDirectory()){
				files.add((IFile) resource);
			}
		}
		return files;
	}

	public Set<IResource> getResources(int statusBits, IContainer folder){
		// Possible optimization: don't walk the entry set. Call folder.accept() and query statusMap
		// individually for each.
		Set<IResource> resources;
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(folder);
		if(hgRoot == null) {
			return Collections.emptySet();
		}
		boolean isMappedState = statusBits != BIT_CLEAN && statusBits != BIT_IMPOSSIBLE
			&& Bits.cardinality(statusBits) == 1;
		if(isMappedState){
			PathsSet set = bitMap.get(statusBits);
			if(set == null || set.isEmpty()){
				return Collections.emptySet();
			}
			IPath parentPath = ResourceUtils.getPath(folder);
			if(parentPath.isEmpty()) {
				return Collections.emptySet();
			}
			resources = new HashSet<IResource>();
			List<IPath> children = set.getChildren(parentPath);
			if(children != null) {
				for (IPath path : children) {
					// TODO try to use container.getFile (performance?)
					// we don't know if it is a file or folder...
					IResource tmp;
					if(isDirectory(path)) {
						tmp = hgRoot.getResource().getFolder(hgRoot.toRelative(path));
					} else {
						tmp = hgRoot.getResource().getFile(hgRoot.toRelative(path));
					}
					if(tmp != null) {
						resources.add(tmp);
					}
				}
			}
		} else {
			resources = new HashSet<IResource>();
			Set<Entry<IPath, Integer>> entrySet = statusMap.entrySet();
			IPath parentPath = ResourceUtils.getPath(folder);
			if(parentPath.isEmpty()) {
				return Collections.emptySet();
			}
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			for (Entry<IPath, Integer> entry : entrySet) {
				Integer status = entry.getValue();
				if(status != null && Bits.contains(status.intValue(), statusBits)){
					IPath path = entry.getKey();
					if(!ResourceUtils.isPrefixOf(parentPath, path)) {
						continue;
					}
					// we don't know if it is a file or folder...
					IPath relative = hgRoot.toRelative(path);
					if(relative.isEmpty()) {
						resources.add(hgRoot.getResource());
						continue;
					}
					IResource tmp;
					if(isDirectory(path)) {
						tmp = hgRoot.getResource().getFolder(relative);
					} else {
						tmp = root.getFileForLocation(path);
						if(tmp != null) {
							if(!ResourceUtils.isPrefixOf(parentPath, path)) {
								tmp = null;
							}
						} else {
							tmp = root.getContainerForLocation(path);
							if(tmp != null) {
								if(!ResourceUtils.isPrefixOf(parentPath, path)) {
									tmp = null;
								} else {
									setStatus(path, status, true);
								}
							}
						}
						if(tmp == null) {
							if(path.toFile().isDirectory()) {
								setStatus(path, status, true);
								tmp = hgRoot.getResource().getFolder(relative);
							} else {
								tmp = hgRoot.getResource().getFile(relative);
							}
						}
					}
					if(tmp != null) {
						resources.add(tmp);
					}
				}
			}
		}
		return resources;
	}

	private static IProgressMonitor checkMonitor(IProgressMonitor monitor){
		if(monitor == null){
			return new NullProgressMonitor();
		}
		return monitor;
	}

	/**
	 * Refreshes the local repository status for all projects under the given hg root
	 *  and notifies the listeners about changes. No refresh of changesets.
	 */
	public void refreshStatus(HgRoot root, IProgressMonitor monitor) throws HgException {
		Assert.isNotNull(root);
		monitor = checkMonitor(monitor);
		monitor.subTask(NLS.bind(Messages.mercurialStatusCache_Refreshing, root.getName()));

		// find all subrepos under the specified root
		// in general we can have several projects under the same root
		// but due to subrepositories we can also have several roots under the same project
		Set<HgRoot> repos;
		if(enableSubrepos){
			repos = HgSubreposClient.findSubrepositoriesRecursively(root);
		} else {
			repos = new HashSet<HgRoot>();
		}
		repos.add(root);

		// find all projects that are under the root and any of its subrepos. Each project can only
		// be under one HgRoot (it can contain more roots, but that's not relevant at this point)
		RootResourceSet<IProject> projects = new RootResourceSet<IProject>();
		for(HgRoot repo : repos){
			for(IProject proj : ResourceUtils.getProjects(repo)){
				projects.add(repo, proj);
			}
		}

		Set<IResource> changed = new HashSet<IResource>();
		synchronized (statusUpdateLock) {

			// build a map of project->projectPath for all projects under the specified root
			// as well as projects under the subrepos of the specified root
			Map<IProject, IPath> pathMap = new HashMap<IProject, IPath>();
			Iterator<IProject> iterator = projects.resourceIterator();
			while (iterator.hasNext()) {
				IProject project = iterator.next();
				if (!project.isOpen() || !MercurialTeamProvider.isHgTeamProviderFor(project)) {
					iterator.remove();
					continue;
				}
				// clear status for project
				if(knownStatus.contains(project)) {
					clear(project, false);
				}
				monitor.worked(1);
				if(monitor.isCanceled()){
					return;
				}
				IPath location = project.getLocation();
				if(location == null) {
					iterator.remove();
					continue;
				}
				pathMap.put(project, location);
			}

			// for the Root and all its subrepos
			// we have to iterate over repos instead of projects.getRoot(), because there may be a single project with lot of subrepos inside
			for(HgRoot repo : repos){
				// get status and branch for hg root
				String output = HgStatusClient.getStatusWithoutIgnored(repo);
				String[] mergeStatus = HgStatusClient.getIdMergeAndBranch(repo);
				String currentChangeSetId = mergeStatus[0];
				LocalChangesetCache.getInstance().checkLatestChangeset(repo, currentChangeSetId);
				String mergeNode = mergeStatus[1];
				String branch = mergeStatus[2];

				// update status of all files in the root that are also contained in projects inside pathMap
				String[] lines = NEWLINE.split(output);
				changed.addAll(parseStatus(repo, pathMap, lines, false));

				MercurialTeamProvider.setCurrentBranch(branch, repo);

				// Set the merge status of the root itself
				setMergeStatus(repo, mergeNode);

				// set the projects status information
				// this will happen exactly once for each project (since each project is only under one root)
				Set<IProject> repoProjects = projects.getResources(repo);
				if(repoProjects != null){
					for (IProject project : repoProjects) {
						knownStatus.add(project);
						setMergeStatus(project, mergeNode);
					}
				}

				changed.addAll(checkForConflict(repo));

				monitor.worked(1);
				if(monitor.isCanceled()){
					return;
				}
			}

			knownStatus.add(root.getResource());
		}

		notifyChanged(changed, false);

		monitor.worked(1);
	}

	/**
	 * Refreshes local repository status and notifies the listeners about changes. No refresh of changesets.
	 */
	public void refreshStatus(IResource res, IProgressMonitor monitor) throws HgException {
		Assert.isNotNull(res);
		monitor = checkMonitor(monitor);
		monitor.subTask(NLS.bind(Messages.mercurialStatusCache_Refreshing, res.getName()));

		IProject project = res.getProject();

		if (!project.isOpen() || !MercurialTeamProvider.isHgTeamProviderFor(res)) {
			return;
		}

		// find all the subrepos that are inside the resource
		Set<HgRoot> repos;
		HgRoot root = MercurialTeamProvider.getHgRoot(res);
		if(enableSubrepos && root != null){
			// find the reposoritory in which the resource is
			repos = HgSubreposClient.findSubrepositoriesRecursivelyWithin(root, res);
		} else {
			repos = new HashSet<HgRoot>();
		}
		if(root != null) {
			repos.add(root);
		}

		Set<IResource> changed = new HashSet<IResource>();
		IPath projectLocation = project.getLocation();
		if(projectLocation == null) {
			return;
		}

		synchronized (statusUpdateLock) {
			// clear status for files, folders or project
			if(res instanceof IProject && knownStatus.contains(project)){
				clear(project, false);
			} else {
				clearStatusCache(res);
			}
			monitor.worked(1);
			if(monitor.isCanceled()){
				return;
			}

			for (HgRoot repo : repos) {

				// Call hg to get the status of the repository
				String output = HgStatusClient.getStatusWithoutIgnored(repo, res);
				monitor.worked(1);
				if(monitor.isCanceled()){
					return;
				}

				// parse the hg result
				String[] lines = NEWLINE.split(output);
				Map<IProject, IPath> pathMap = new HashMap<IProject, IPath>();
				pathMap.put(project, projectLocation);
				changed.addAll(parseStatus(repo, pathMap, lines, !(res instanceof IProject)));
				if( !(res instanceof IProject) && !changed.contains(res)){
					// fix for issue 10155: No status update after reverting changes on .hgignore
					changed.add(res);
					if(res instanceof IFolder){
						IFolder folder = (IFolder) res;
						ResourceUtils.collectAllResources(folder, changed);
					}
				}

				// refresh the status of the HgRoot we are processing
				try {
					if(res instanceof IProject || repo != root){
						String[] mergeStatus = HgStatusClient.getIdMergeAndBranch(repo);
						String id = mergeStatus[0];
						LocalChangesetCache.getInstance().checkLatestChangeset(repo, id);
						String mergeNode = mergeStatus[1];
						String branch = mergeStatus[2];
						setMergeStatus(repo, mergeNode);
						MercurialTeamProvider.setCurrentBranch(branch, repo);
						if(repo == root){
							// the project is under the current HgRoot, update its status as well
							setMergeStatus((IProject)res, mergeNode);
						}
					}
				} catch (HgException e) {
					throw new HgException(Messages.mercurialStatusCache_FailedToRefreshMergeStatus, e);
				}
			}

			if(res instanceof IProject){
				knownStatus.add(project);
			}
		}
		if(monitor.isCanceled()){
			return;
		}
		monitor.worked(1);

		// TODO shouldn't this go in the block above?
		changed.addAll(checkForConflict(project));
		if(monitor.isCanceled()){
			return;
		}
		monitor.worked(1);
		notifyChanged(changed, false);

		monitor.worked(1);
	}

	/**
	 * @param res
	 * @return true if a change of given file can trigger a project status update
	 * @throws HgException
	 */
	public static boolean canTriggerFullCacheUpdate(IResource res) throws HgException {
		if(!(res instanceof IFile)){
			return false;
		}
		return ".hgignore".equals(res.getName());
	}

	/**
	 * @param folder non null resource
	 * @return non null set of all child entries managed by this cache
	 */
	private Set<IPath> getChildrenFromCache(IContainer folder) {
		IPath parentPath = ResourceUtils.getPath(folder);
		return getPathChildrenFromCache(parentPath);
	}

	/**
	 * @param parentPath
	 * @return non null set of all child entries managed by this cache
	 */
	private Set<IPath> getPathChildrenFromCache(IPath parentPath) {
		Set<IPath> children = new HashSet<IPath>();
		// empty or root paths shouldn't be tracked.
		if(parentPath.isEmpty()) {
			return children;
		}
		Set<IPath> keySet = statusMap.keySet();
		for (IPath path : keySet) {
			if(path != null && ResourceUtils.isPrefixOf(parentPath, path)) {
				children.add(path);
			}
		}
		children.remove(parentPath);
		return children;
	}

	private Set<IResource> checkForConflict(final IProject project) throws HgException {

		List<FlaggedAdaptable> status = HgResolveClient.list(project);
		Set<IResource> changed = new HashSet<IResource>();
		Set<IResource> members = getLocalMembers(project);
		for (IResource res : members) {
			if(removeConflict(res.getLocation())){
				changed.add(res);
			}
		}
		if(removeConflict(project.getLocation())){
			changed.add(project);
		}
		for (FlaggedAdaptable flaggedAdaptable : status) {
			IFile file = (IFile) flaggedAdaptable.getAdapter(IFile.class);
			if (flaggedAdaptable.getFlag() == CHAR_UNRESOLVED && file != null) {
				changed.addAll(addConflict(file));
			}
		}
		return changed;
	}

	private Set<IResource> checkForConflict(final HgRoot hgRoot) throws HgException {

		List<FlaggedAdaptable> status = HgResolveClient.list(hgRoot);
		Set<IResource> changed = new HashSet<IResource>();
		IPath parentPath = new Path(hgRoot.getAbsolutePath());
		List<IPath> members = getPaths(BIT_CONFLICT, parentPath);
		if(members != null){
			for (int i = 0; i < members.size(); i++) {
				IPath childPath = members.get(i);
				if(removeConflict(childPath)){
					IFile fileHandle = ResourceUtils.getFileHandle(childPath);
					if(fileHandle != null) {
						changed.add(fileHandle);
					}
				}
			}
		}
		for (FlaggedAdaptable flaggedAdaptable : status) {
			IFile file = (IFile) flaggedAdaptable.getAdapter(IFile.class);
			if (flaggedAdaptable.getFlag() == CHAR_UNRESOLVED && file != null) {
				changed.addAll(addConflict(file));
			}
		}
		return changed;
	}

	private static final Pattern NEWLINE = Pattern.compile("\n");

	/**
	 * @param lines must contain file paths as paths relative to the hg root
	 * @param pathMap multiple projects (from this hg root) as input
	 * @param propagateAllStates true to propagate all changes in children states to parents,
	 * e.g. both transition from clean to dirty state and from dirty to clean state.
	 * If false, then only dirty state is propagated to parents.
	 *
	 * @return set with resources to refresh
	 */
	private Set<IResource> parseStatus(HgRoot root, Map<IProject, IPath> pathMap, String[] lines,
			boolean propagateAllStates) {
		long start = 0;
		if(debug){
			start = System.currentTimeMillis();
		}
		// we need the project for performance reasons - gotta hand it to
		// addToProjectResources
		Set<IResource> changed = new HashSet<IResource>();
		List<String> strangeStates = new ArrayList<String>();

		// Make values in the path map canonical
		try {
			for (Iterator<IProject> it = pathMap.keySet().iterator(); it.hasNext();) {
				IProject key = it.next();
				pathMap.put(key, Path.fromOSString(pathMap.get(key).toFile().getCanonicalPath()));
			}
		} catch(IOException e) {
			MercurialEclipsePlugin.logError("Unexpected error - paths should be canonicalizable", e);
		}

		for (String line : lines) {
			if(line.length() <= 2){
				strangeStates.add(line);
				continue;
			}

			char space = line.charAt(1);
			int bit = getBit(line.charAt(0));
			if(bit == BIT_IMPOSSIBLE || space != ' '){
				strangeStates.add(line);
				continue;
			}
			String localName = line.substring(2);
			IResource member = findMember(pathMap, root, localName, bit == BIT_REMOVED || bit == BIT_MISSING);

			// doesn't belong to our project (can happen if root is above project level)
			// or simply deleted, so can't be found...
			if (member == null) {
				continue;
			}

			Integer bitSet;
			if (bit == BIT_UNKNOWN && Team.isIgnoredHint(member)) {
				bitSet = IGNORE;
			} else {
				bitSet = Integer.valueOf(bit);
				changed.add(member);
			}
			if(!member.isLinked(IResource.CHECK_ANCESTORS)) {
				setStatus(member.getLocation(), bitSet, member.getType() == IResource.FOLDER);
				changed.addAll(setStatusToAncestors(member, bitSet, propagateAllStates));
			}
		}
		if(debug && strangeStates.size() > 0){
			IStatus [] states = new IStatus[strangeStates.size()];
			for (int i = 0; i < states.length; i++) {
				states[i] = MercurialEclipsePlugin.createStatus(strangeStates.get(i), IStatus.OK, IStatus.INFO, null);
			}
			String message = "Strange status received from hg";
			MultiStatus st = new MultiStatus(MercurialEclipsePlugin.ID, IStatus.OK, states,
					message, new Exception(message));
			MercurialEclipsePlugin.getDefault().getLog().log(st);
		}
		if(debug){
			System.out.println("Parse status took: " + (System.currentTimeMillis() - start));
		}
		return changed;
	}

	/**
	 * Parse status output. Future: merge with above method?
	 *
	 * @param status
	 *            Status output
	 * @param hgRoot
	 *            The root to use for the {@link FileStatus}
	 * @return A non-null list.
	 */
	public static List<FileStatus> parseStatus(String status, HgRoot hgRoot) {
		List<FileStatus> list = new ArrayList<FileStatus>();

		for (String line : NEWLINE.split(status)) {
			if (line.length() <= 2 || line.charAt(1) != ' ') {
				continue;
			}

			char c = line.charAt(0);
			FileStatus.Action action;

			if (c == CHAR_ADDED) {
				action = FileStatus.Action.ADDED;
			} else if (c == CHAR_REMOVED) {
				action = FileStatus.Action.REMOVED;
			} else if (c == CHAR_MODIFIED) {
				action = FileStatus.Action.MODIFIED;
			} else {
				continue;
			}

			list.add(new FileStatus(action, line.substring(2), hgRoot));
		}

		return list;
	}

	/**
	 * @return return null if resource is not known or linked and not under the same root
	 */
	private static IResource findMember(Map<IProject, IPath> pathMap, final HgRoot hgRoot,
			final String repoRelPath, final boolean allowForce) {
		IPath hgRootPath = hgRoot.getIPath();
		// determine absolute path
		IPath path = hgRootPath.append(repoRelPath);
		Set<Entry<IProject, IPath>> set = pathMap.entrySet();
		for (Entry<IProject, IPath> entry : set) {
			IPath projectLocation = entry.getValue();
			// determine project relative path
			int equalSegments = path.matchingFirstSegments(projectLocation);
			if(equalSegments == projectLocation.segmentCount() || hgRootPath.equals(projectLocation)) {
				IProject project = entry.getKey();
				IPath segments = path.removeFirstSegments(equalSegments);
				IResource result = project.findMember(segments);

				if (result == null && allowForce) {
					result = project.getFile(segments);
				}
				return result;
			}
		}
		IPath rel = new Path(repoRelPath);
		if(allowForce) {
			return hgRoot.getResource().getFile(rel);
		}
		return hgRoot.getResource().findMember(rel);
	}

	private void setStatus(IPath location, Integer status, boolean isDir) {
		if(location == null || location.isEmpty()){
			return;
		}
		statusMap.put(location, status);
		bitMap.put(location, status);
		if(isDir){
			bitMap.put(location, Integer.valueOf(BIT_DIR));
		}
	}

	/**
	 *
	 * @param child
	 * @param childState
	 * @param propagateAllStates true to propagate all changes in children states to parents,
	 * e.g. both transition from clean to dirty state and from dirty to clean state.
	 * If false, then only dirty state is propagated to parents.
	 * @return
	 */
	private Set<IResource> setStatusToAncestors(IResource child, Integer childState, boolean propagateAllStates) {
		Set<IResource> ancestors = new HashSet<IResource>();
		IContainer parent = child.getParent();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		for (; parent != null && parent != root; parent = parent.getParent()) {
			IPath parentLocation = parent.getLocation();
			if(parentLocation == null){
				continue;
			}
			int parentBitSet = BIT_CLEAN;
			Integer parentBits = statusMap.get(parentLocation);
			if(parentBits != null){
				parentBitSet = parentBits.intValue();
			}
			int childBitSet = childState.intValue();

			// should not propagate ignores states to parents
			// TODO issue 237: "two status feature"
			boolean childIsDirty = Bits.contains(childBitSet, MODIFIED_MASK);
			childBitSet = Bits.clear(childBitSet, IGNORED_MASK);
			if(childIsDirty) {
				childBitSet |= BIT_MODIFIED;
			} else {
				childBitSet |= BIT_CLEAN;
			}

			if (child.getType() == IResource.PROJECT) {
				childBitSet |= parentBitSet;
			} else if (!childIsDirty) {
				// child is clean, and we have "usual" files and folders
				if (!propagateAllStates) {
					if(parentBits != null){
						// parent status known: just exit here. Saves us A LOT of time
						return ancestors;
					}
				} else {
					// propagate clean state back to parents - e.g. if file was reverted,
					// and there are NO OTHER dirty children, parent state should change to "clean"
					if (parent.isAccessible() && !parent.isTeamPrivateMember()) {
						MemberStatusVisitor visitor = new MemberStatusVisitor(parentLocation, childBitSet);
						// we have to traverse all possible "dirty" children and change
						// parent state from "dirty" to "clean"...
						boolean visit = checkChildrenFor(parentLocation, visitor, BIT_MODIFIED);
						if (visit) {
							visit = checkChildrenFor(parentLocation, visitor, BIT_UNKNOWN);
						}
						if (visit) {
							visit = checkChildrenFor(parentLocation, visitor, BIT_ADDED);
						}
						if (visit) {
							visit = checkChildrenFor(parentLocation, visitor, BIT_REMOVED);
						}
						if (visit) {
							visit = checkChildrenFor(parentLocation, visitor, BIT_MISSING);
						}
						childBitSet = visitor.bitSet;
					}
				}
			}
			setStatus(parentLocation, Integer.valueOf(childBitSet), parent.getType() == IResource.FOLDER);
			ancestors.add(parent);
		}
		return ancestors;
	}

	private boolean checkChildrenFor(IPath location, MemberStatusVisitor visitor, int stateBit) {
		List<IPath> resources = getDirectChildren(stateBit, location);
		if(resources == null){
			return true;
		}
		for (int i = 0; i < resources.size(); i++) {
			IPath child = resources.get(i);
			boolean continueVisit = visitor.visit(child);
			if(!continueVisit){
				return false;
			}
		}
		return true;
	}

	private static int getBit(char status) {
		switch (status) {
		case CHAR_MISSING:
			return BIT_MISSING;
		case CHAR_REMOVED:
			return BIT_REMOVED;
		case CHAR_IGNORED:
			return BIT_IGNORE;
		case CHAR_CLEAN:
			return BIT_CLEAN;
		case CHAR_UNKNOWN:
			return BIT_UNKNOWN;
		case CHAR_ADDED:
			return BIT_ADDED;
		case CHAR_MODIFIED:
			return BIT_MODIFIED;
		default:
			return BIT_IMPOSSIBLE;
		}
	}

	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
			return;
		}
		IResourceDelta delta = event.getDelta();

		final Map<IProject, RootResourceSet<IResource>> changed = new HashMap<IProject, RootResourceSet<IResource>>();
		final Map<IProject, RootResourceSet<IResource>> added = new HashMap<IProject, RootResourceSet<IResource>>();
		final Map<IProject, RootResourceSet<IResource>> removed = new HashMap<IProject, RootResourceSet<IResource>>();

		IResourceDeltaVisitor visitor = new ResourceDeltaVisitor(removed, changed, added);

		try {
			// walk tree
			delta.accept(visitor);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return;
		}

		final Set<IProject> changedProjects = new HashSet<IProject>(changed.keySet());
		changedProjects.addAll(added.keySet());
		changedProjects.addAll(removed.keySet());
		for (IProject project : changedProjects) {
			RootResourceSet<IResource> addedSet = added.get(project);
			RootResourceSet<IResource> removedSet = removed.get(project);
			RootResourceSet<IResource> changedSet = changed.get(project);

			projectChanged(project, addedSet, removedSet, changedSet);
		}

	}

	private void projectChanged(final IProject project, final RootResourceSet<IResource> addedSet, final RootResourceSet<IResource> removedSet,
			final RootResourceSet<IResource> changedSet) {
		ProjectUpdateJob updateJob = new ProjectUpdateJob(removedSet, changedSet, project, addedSet);
		Job[] jobs = Job.getJobManager().find(ProjectUpdateJob.class);
		for (Job job : jobs) {
			if(updateJob.equals(job)){
				job.cancel();
				if(debug){
					System.out.println("Status cache update cancelled for: "
							+ ((ProjectUpdateJob) job).project.getName());
				}
			}
		}
		// schedule async and with delay to avoid multiple refreshes on the same subject
		// do not join in the resource notification loop
		updateJob.schedule(300);
	}

	/**
	 * Refreshes Status of resources in batches and notifies the listeners about changes
	 *
	 * @param resources
	 *            may be null. If not null, then all elements must be from the given project. If null, no refresh will
	 *            happen. If the set contains a project, it is ignored
	 * @param project
	 *            not null. The project which resources state has to be updated
	 */
	private void refreshStatus(final RootResourceSet<IResource> resources, IProject project) throws HgException {
		if (resources == null || resources.isEmpty()) {
			return;
		}
		// project status wanted, no batching needed
		if(resources.remove(project) && resources.isEmpty()){
			return;
			}

		Set<IResource> changed = new HashSet<IResource>();
		for(Map.Entry<HgRoot, Set<IResource>> entry : resources.entrySet()){
			changed.addAll(updateStatusInRoot(project, entry.getKey(), entry.getValue()));
		}

		if(!resources.isEmpty()) {
			changed.addAll(checkForConflict(project));
		}
		notifyChanged(changed, false);
		return;
	}

	private Set<IResource> updateStatusInRoot(IProject project, HgRoot root,
			Set<IResource> resources) throws HgException {
		int batchSize = getStatusBatchSize();
		List<IResource> currentBatch = new ArrayList<IResource>();
		Set<IResource> changed = new HashSet<IResource>();

		boolean listFileEnabled = HgFeatures.LISTFILE.isEnabled();
		for (Iterator<IResource> iterator = resources.iterator(); iterator.hasNext();) {
			IResource resource = iterator.next();

			// status for single resource is batched
			if (!resource.isTeamPrivateMember()) {
				currentBatch.add(resource);
			}
			if(!listFileEnabled) {
			if (currentBatch.size() % batchSize == 0 || !iterator.hasNext()) {
				// call hg with batch
					updateStatusBatched(project, root, currentBatch, changed);
					currentBatch.clear();
				}
			}
		}

		if(listFileEnabled) {
			updateStatusBatched(project, root, currentBatch, changed);
		}

		return changed;
	}

	private void updateStatusBatched(IProject project, HgRoot root, List<IResource> currentBatch,
			Set<IResource> changed) throws HgException {
				synchronized (statusUpdateLock) {
					for (IResource curr : currentBatch) {
						boolean unknown = (curr instanceof IContainer) || isUnknown(curr);
						clearStatusCache(curr);
						if (unknown && !curr.exists()) {
							// remember parents of deleted files: we must update their state
							IContainer directory = ResourceUtils.getFirstExistingDirectory(curr);
							while(directory != null) {
								changed.add(directory);
								IPath parentPath = directory.getLocation();
								if(parentPath != null) {
									bitMap.remove(parentPath);
									statusMap.remove(parentPath);
								}
								directory = ResourceUtils.getFirstExistingDirectory(directory.getParent());
							}
							// recursive recalculate parents state
							// TODO better to combine it with parse status below...
							setStatusToAncestors(curr, CLEAN, true);
						}
					}
					String output = HgStatusClient.getStatusWithoutIgnored(root, currentBatch);
					String[] lines = NEWLINE.split(output);
					Map<IProject, IPath> pathMap = new HashMap<IProject, IPath>();
					IPath projectLocation = project.getLocation();
					if(projectLocation != null) {
						pathMap.put(project, projectLocation);
					}
					changed.addAll(parseStatus(root, pathMap, lines, true));
				}
	}

	public void clearStatusCache(IResource resource) {
		if(resource instanceof IProject && !resource.exists()) {
			return;
		}
		IPath parentPath = ResourceUtils.getPath(resource);
		synchronized (statusUpdateLock) {
			if(resource instanceof IContainer && !parentPath.isEmpty()){
				// same can be done via getChildrenFromCache(resource), but we
				// iterating/removing over keyset directly to reduce memory consumption
				Set<IPath> entrySet = statusMap.keySet();
				Iterator<IPath> it = entrySet.iterator();
				while (it.hasNext()) {
					IPath path = it.next();
					if(path != null && ResourceUtils.isPrefixOf(parentPath, path)) {
						it.remove();
						bitMap.remove(path);
					}
				}
			} else {
				bitMap.remove(parentPath);
				statusMap.remove(parentPath);
			}
		}
	}

	private int getStatusBatchSize() {
		return statusBatchSize;
	}

	/**
	 * @param resource
	 * @return never null. Set will contain all known files under the given directory,
	 * or the file itself if given resource is not a directory
	 */
	public Set<IResource> getLocalMembers(IResource resource) {
		Set<IResource> members = new HashSet<IResource>();
		if(resource instanceof IContainer){
			IContainer container = (IContainer) resource;
			IPath location = container.getLocation();
			if(location == null) {
				return members;
			}
			int segmentCount = location.segmentCount();
			Set<IPath> children = getChildrenFromCache(container);
			for (IPath path : children) {
				IFile iFile = container.getFile(path.removeFirstSegments(segmentCount));
				if(iFile != null) {
					members.add(iFile);
				}
			}
		} else {
			members.add(resource);
		}
		return members;
	}

	@Override
	protected void projectDeletedOrClosed(IProject project) {
		clear(project, false);

		// dirty fix for issue 14113: various actions fail for recursive projects
		// if the root project is closed: we simply refresh the state for remaining projects
		IPath path = project.getLocation();
		if(path == null) {
			return;
		}
		Collection<HgRoot> hgRoots = MercurialRootCache.getInstance().getKnownHgRoots();
		for (HgRoot hgRoot : hgRoots) {
			// only start refresh for projects located at the repository root
			if(!hgRoot.getIPath().equals(path)) {
				continue;
			}
			List<IProject> projects = MercurialTeamProvider.getKnownHgProjects(hgRoot);
			projects.remove(project);
			// only start refresh if there is at least one project more in the repo
			if(projects.size() > 0) {
				new RefreshStatusJob("Status update", hgRoot).schedule();
			}
		}

	}

	public void clear(HgRoot root, boolean notify) {
		Set<IProject> projects = ResourceUtils.getProjects(root);
		clearMergeStatus(root.getIPath());
		for (IProject project : projects) {
			clear(project, false);
			if(notify) {
				notifyChanged(project, false);
			}
		}
	}

	public void clear(IProject project, boolean notify) {
		clearMergeStatus(project);
		clearStatusCache(project);
		knownStatus.remove(project);
		if(notify) {
			notifyChanged(project, false);
		}
	}

	/**
	 * Sets conflict marker on resource status
	 */
	private Set<IResource> addConflict(IResource local) {
		IPath location = local.getLocation();
		if(location == null){
			return Collections.emptySet();
		}
		Integer status = statusMap.get(location);
		boolean isDir = local.getType() == IResource.FOLDER;
		if(status == null){
			status = CONFLICT;
			setStatus(location, CONFLICT, isDir);
		} else {
			status = Integer.valueOf(status.intValue() | BIT_CONFLICT);
			setStatus(location, status, isDir);
		}
		Set<IResource> changed = setStatusToAncestors(local, status, false);
		changed.add(local);
		return changed;
	}

	/**
	 * Removes conflict marker on resource status
	 *
	 * @param local non null
	 * @return true if there was a conflict and now it is removed
	 */
	private boolean removeConflict(IPath local) {
		if(local == null){
			return false;
		}
		Integer statusInt = statusMap.get(local);
		if(statusInt == null){
			return false;
		}
		int status = statusInt.intValue();
		if(Bits.contains(status, BIT_CONFLICT)) {
			status = Bits.clear(status, BIT_CONFLICT);
			setStatus(local, Integer.valueOf(status), false);
			return true;
		}
		return false;
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store){
		enableSubrepos = store.getBoolean(MercurialPreferenceConstants.PREF_ENABLE_SUBREPO_SUPPORT);
		// TODO: group batches by repo root

		statusBatchSize = store.getInt(MercurialPreferenceConstants.STATUS_BATCH_SIZE); // STATUS_BATCH_SIZE;
		if (statusBatchSize <= 0) {
			store.setValue(MercurialPreferenceConstants.STATUS_BATCH_SIZE, STATUS_BATCH_SIZE);
			statusBatchSize = STATUS_BATCH_SIZE;
			MercurialEclipsePlugin.logWarning(Messages.mercurialStatusCache_BatchSizeForStatusCommandNotCorrect, null);
		}
	}

	private void clearMergeStatus(IPath path) {
		mergeChangesetIds.remove(path);
	}

	public void clearMergeStatus(IProject res) {
		// clear merge status in Eclipse
		IPath location = res.getLocation();
		if(location != null) {
			mergeChangesetIds.remove(location);
		}
	}

	public void setMergeStatus(HgRoot hgRoot, String mergeChangesetId) {
		Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
		for (IProject project : projects) {
			// clear merge status in Eclipse
			setMergeStatus(project, mergeChangesetId);
		}
		setMergeStatus(hgRoot.getIPath(), mergeChangesetId);
	}

	private void setMergeStatus(IPath path, String mergeChangesetId) {
		if(mergeChangesetId != null){
			mergeChangesetIds.put(path, mergeChangesetId);
		} else{
			// ConcurrentHashMap doesn't support null values, but removing is the same a putting a null value
			mergeChangesetIds.remove(path);
		}
	}

	private void setMergeStatus(IProject project, String mergeChangesetId) {
		// set merge status in Eclipse
		IPath location = project.getLocation();
		if(location == null) {
			return;
		}
		if(mergeChangesetId != null){
			mergeChangesetIds.put(location, mergeChangesetId);
		}else{
			// ConcurrentHashMap doesn't support null values, but removing is the same a putting a null value
			mergeChangesetIds.remove(location);
		}
	}

	public boolean isMergeInProgress(IResource res) {
		return getMergeChangesetId(res.getProject()) != null;
	}

	public boolean isMergeInProgress(HgRoot hgRoot) {
		return getMergeChangesetId(hgRoot) != null;
	}

	/**
	 * @param path A full, absolute path relative to the workspace. non null
	 * @return the version:short_changeset_id OR full_changeset_id string if the root is being merged, otherwise null
	 */
	public String getMergeChangesetId(IPath path){
		return mergeChangesetIds.get(path);
	}

	/**
	 * @param project non null
	 * @return the version:short_changeset_id OR full_changeset_id string if the root is being merged, otherwise null
	 */
	public String getMergeChangesetId(IResource project) {
		IPath location = project.getLocation();
		if(location == null) {
			return null;
		}
		return getMergeChangesetId(location);
	}

	/**
	 * @param hgRoot non null
	 * @return the version:short_changeset_id OR full_changeset_id string if the root is being merged, otherwise null
	 */
	public String getMergeChangesetId(HgRoot hgRoot) {
		Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
		if(!projects.isEmpty()) {
			return getMergeChangesetId(projects.iterator().next());
		}
		return null;
	}

	/**
	 * Determine if the given file is currently in conflict because of a workspace update, ie
	 * not a normal merge or rebase.
	 *
	 * @param file
	 *            The file to check
	 * @return True if the file is in conflict and neither a rebase or merge is in progress.
	 */
	public boolean isWorkspaceUpdateConfict(IFile file) {
		if (isConflict(file)) {
			// Ideally we would save more state so we know what mode we are actually in. For now
			// just check we're not merging or rebasing. Transplant conflicts don't set files in
			// conflict mode. Are there other modes?
			HgRoot root = MercurialTeamProvider.getHgRoot(file);
			if (root != null && !isMergeInProgress(root) && !HgRebaseClient.isRebasing(root)) {
				return true;
			}
		}
		return false;
	}
}
