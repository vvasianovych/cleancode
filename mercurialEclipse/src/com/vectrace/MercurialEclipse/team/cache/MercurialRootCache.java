/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		npiguet				- implementation
 *		John Peberdy 		- refactoring
 *		Andrei Loskutov     - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.core.TeamPlugin;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgResource;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This handles the known roots cache. The caching for an individual resources is handled as a
 * session property on the resource.
 *
 * @author npiguet
 */
public class MercurialRootCache extends AbstractCache {

	private static final QualifiedName SESSION_KEY = new QualifiedName(MercurialEclipsePlugin.ID,
			"MercurialRootCacheKey");

	/**
	 * The current sentinel for no root
	 */
	private String noRoot = "No Mercurial root";

	private final ConcurrentHashMap<HgRoot, HgRoot> knownRoots = new ConcurrentHashMap<HgRoot, HgRoot>(
			16, 0.75f, 4);

	/**
	 * Tracks known non-canonical forms of paths.
	 *
	 * There is a concurrent tree map in Java 1.6+ only.
	 */
	private final TreeMap<IPath, Set<IPath>> canonicalMap = new TreeMap<IPath, Set<IPath>>(
			new Comparator<IPath>() {
				public int compare(IPath o1, IPath o2) {
					for (int i = 0, n = Math.max(o1.segmentCount(), o2.segmentCount()); i < n; i++) {
						String a = o1.segment(i), b = o2.segment(i);
						int res;

						if (a == null) {
							res = b == null ? 0 : -1;
						} else if (b == null) {
							res = 1;
						} else {
							res = a.compareTo(b);
						}

						if (res != 0) {
							return res;
						}
					}
					return 0;
				}
			});

	private MercurialRootCache() {
	}

	public HgRoot calculateHgRoot(IResource resource) {
		File fileHandle = ResourceUtils.getFileHandle(resource);
		if(fileHandle.getPath().length() == 0) {
			return null;
		}
		return calculateHgRoot(fileHandle, false);
	}

	private HgRoot calculateHgRoot(File file, boolean reportNotFoundRoot) {
		if (file instanceof HgRoot) {
			return (HgRoot) file;
		}

		// TODO: possible optimization: try to look for the parent in the cache, or load the whole hierarchy in cache
		//       or something else like that, so we don't need to call HgRootClient for each file in a directory
		HgRoot root;

		try {
			root = HgRootClient.getHgRoot(file);
		} catch (HgException e) {
			if(reportNotFoundRoot) {
				MercurialEclipsePlugin.logError(e);
			}
			// no root found at all
			root = null;
		}

		if (root != null) {
			HgRoot prev = knownRoots.putIfAbsent(root, root);

			if (prev != null) {
				root = prev;
			}
		}

		return root;
	}

	/**
	 * Find the hg root for the given resource. If the root could not be found, no error would be
	 * reported.
	 *
	 * @param resource
	 *            The resource, not null.
	 * @param resolveIfNotKnown
	 *            true to trigger hg root search or/and also possible team provider configuration
	 *            operation, which may lead to locking
	 *
	 * @return The hg root, or null if an error occurred or enclosing project is closed or project
	 *         team provider is not Mercurial or hg root is not found
	 */
	public HgRoot hasHgRoot(IResource resource, boolean resolveIfNotKnown) {
		return hasHgRoot(resource, resolveIfNotKnown, false);
	}

	/**
	 * Find the hg root for the given resource. If the root could not be found, an error will be
	 * reported.
	 *
	 * @param resource
	 *            The resource, not null.
	 * @param resolveIfNotKnown
	 *            true to trigger hg root search or/and also possible team provider configuration
	 *            operation, which may lead to locking
	 * @param reportNotFoundRoot
	 * 		      true to report an error if the root is not found
	 *
	 * @return The hg root, or null if an error occurred or enclosing project is closed or project
	 *         team provider is not Mercurial or hg root is not found
	 */
	public HgRoot hasHgRoot(IResource resource, boolean resolveIfNotKnown, boolean reportNotFoundRoot) {
		return getHgRoot(resource, resolveIfNotKnown, reportNotFoundRoot);
	}

	/**
	 * Find the hg root for the given resource.
	 *
	 * @param resource
	 *            The resource, not null.
	 * @param resolveIfNotKnown
	 *            true to trigger hg root search or/and also possible team provider configuration
	 *            operation, which may lead to locking
	 * @param reportNotFoundRoot
	 * 		      true to report an error if the root is not found
	 * @return The hg root, or null if an error occurred or enclosing project is closed or project
	 *         team provider is not Mercurial or hg root is not found
	 */
	private HgRoot getHgRoot(IResource resource, boolean resolveIfNotKnown, boolean reportNotFoundRoot) {
		if (resource instanceof IHgResource) {
			// special case for HgRootContainers, they already know their HgRoot
			return ((IHgResource) resource).getHgRoot();
		}

		IProject project = resource.getProject();
		if(project == null) {
			return null;
		}

		// The call to RepositoryProvider is needed to trigger configure(project) on
		// MercurialTeamProvider if it doesn't happen before. Additionally, we avoid the
		// case if the hg root is there but project is NOT configured for MercurialEclipse
		// as team provider. See issue 13448.
		if(resolveIfNotKnown) {
			boolean projectIsOpen = project.isOpen();
			if(!projectIsOpen) {
				IPath path = ResourceUtils.getPath(project);
				if(canonicalMap.containsKey(path)) {
					return knownRoots.get(path.toFile());
				} else if(path.segmentCount() > 1) {
					// last try: the root directory of the project?
					path = path.removeLastSegments(1);
					if(canonicalMap.containsKey(path)) {
						return knownRoots.get(path.toFile());
					}
				}
			}

			RepositoryProvider provider = RepositoryProvider.getProvider(project,
					MercurialTeamProvider.ID);
			if (!(provider instanceof MercurialTeamProvider)) {
				if(provider == null && projectIsOpen && project.isHidden()) {
					Object cachedRoot;
					try {
						cachedRoot = project.getSessionProperty(SESSION_KEY);
						if(cachedRoot instanceof HgRoot) {
							return (HgRoot) cachedRoot;
						}
					} catch (CoreException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
				return null;
			}
		} else {
			if(!isHgTeamProviderFor(project)){
				return null;
			}
		}

		// As an optimization only cache for containers not files
		if (resource instanceof IFile && !resource.isLinked()) {
			resource = resource.getParent();
		}

		boolean cacheResult = true;
		try {
			Object cachedRoot = resource.getSessionProperty(SESSION_KEY);
			if(cachedRoot instanceof HgRoot) {
				return (HgRoot) cachedRoot;
			}
			if (cachedRoot == noRoot) {
				return null;
			}
		} catch (CoreException e) {
			// Possible reasons:
			// - This resource does not exist.
			// - This resource is not local.
			cacheResult = false;
		}

		if(!resolveIfNotKnown) {
			return null;
		}
		// cachedRoot can be only null or an obsolete noRoot object
		File fileHandle = ResourceUtils.getFileHandle(resource);
		if(fileHandle.getPath().length() == 0) {
			return null;
		}
		HgRoot root = calculateHgRoot(fileHandle, reportNotFoundRoot);
		if (cacheResult) {
			try {
				markAsCached(resource, root);

				if (root != null) {
					synchronized (canonicalMap) {
						Set<IPath> s = canonicalMap.get(root.getIPath());
						if (s == null) {
							canonicalMap.put(root.getIPath(), s = new HashSet<IPath>());
						}
						IPath projectPath = project.getLocation();
						if (!resource.isLinked(IResource.CHECK_ANCESTORS)
								&& !root.getIPath().equals(projectPath)
								&& !root.getIPath().isPrefixOf(projectPath)) {
							// only add paths which are *different* and NOT children of the root
							s.add(projectPath);
						}
					}
				}
			} catch (CoreException e) {
				// Possible reasons:
				// - 2 reasons above, or
				// - Resource changes are disallowed during certain types of resource change event
				// notification. See IResourceChangeEvent for more details.
				if(reportNotFoundRoot) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}
		return root;
	}

	public static void markAsCached(IResource resource, HgRoot root) throws CoreException {
		Object value = root == null ? getInstance().noRoot : root;
		resource.setSessionProperty(SESSION_KEY, value);
		if(root == null) {
			// mark all parents up to project as NOT in Mercurial
			while(!(resource.getParent() instanceof IWorkspaceRoot) && !resource.isLinked()){
				resource = resource.getParent();
				if(value.equals(resource.getSessionProperty(SESSION_KEY))) {
					return;
				}
				resource.setSessionProperty(SESSION_KEY, value);
			}
		} else {
			// only process if there are no links etc, means the root location
			// can be properly detected by simple path compare
			if(root.getIPath().isPrefixOf(resource.getLocation())){
				// mark all parents up to the root location as IN Mercurial
				while(!(resource.getParent() instanceof IWorkspaceRoot) && !resource.isLinked()
						&& !root.getIPath().equals(resource.getLocation())){
					resource = resource.getParent();
					if(value.equals(resource.getSessionProperty(SESSION_KEY))) {
						return;
					}
					resource.setSessionProperty(SESSION_KEY, value);
				}
			}
		}
	}

	/**
	 * Checks if the given project is controlled by MercurialEclipse
	 * as team provider. This method does not access any locks and so can be called
	 * from synchronized code.
	 *
	 * @param project
	 *            non null
	 * @return true, if MercurialEclipse provides team functions to this project, false otherwise
	 *         (if an error occurred or project is closed).
	 */
	@SuppressWarnings("restriction")
	public static boolean isHgTeamProviderFor(IProject project){
		Assert.isNotNull(project);
		try {
			if(!project.isAccessible()) {
				return false;
			}
			Object provider = project.getSessionProperty(TeamPlugin.PROVIDER_PROP_KEY);
			return provider instanceof MercurialTeamProvider;
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return false;
		}
	}


	/**
	 * Find the hgroot for the given resource.
	 *
	 * @param resource The resource, not null.
	 * @return The hgroot, or null if an error occurred or not found
	 */
	public HgRoot getHgRoot(IResource resource) {
		return getHgRoot(resource, true, true);
	}

	public Collection<HgRoot> getKnownHgRoots(){
		return new ArrayList<HgRoot>(this.knownRoots.values());
	}

	@Override
	protected void configureFromPreferences(IPreferenceStore store) {
		// nothing to do
	}

	@Override
	public void projectDeletedOrClosed(IProject project) {
		if(!project.exists()) {
			return;
		}
		HgRoot hgRoot = getHgRoot(project, false, false);
		if(hgRoot == null) {
			return;
		}

		List<IProject> projects = MercurialTeamProvider.getKnownHgProjects(hgRoot);

		// Fix for issue 14094: Refactor->Rename project throws lots of errors
		uncache(project);

		if(projects.size() > 1 && hgRoot.getIPath().equals(project.getLocation())) {
			// See 14113: various actions fail for recursive projects if the root project is closed
			// do not remove root as there are more then one project inside
			return;
		}

		IPath projPath = ResourceUtils.getPath(project);
		if (!projPath.isEmpty()) {
			Iterator<HgRoot> it = knownRoots.values().iterator();
			while (it.hasNext()) {
				HgRoot root = it.next();
				if (projPath.isPrefixOf(root.getIPath())) {
					it.remove();
				}
			}
		}
	}

	/**
	 * When there are changes done outside of eclipse the root of a resource may go away or change.
	 *
	 * @param resource
	 *            The resource to evict.
	 */
	public static void uncache(IResource resource) {
		// A different more efficient approach would be to mark all contained hgroots (or just all
		// known root) as obsolete and then when a resource is queried we can detect this and
		// discard the cached result thereby making the invalidation lazy. But that would make
		// things more complex so use brute force for now:
		try {
			resource.accept(new IResourceVisitor() {
				public boolean visit(IResource res) throws CoreException {
					res.setSessionProperty(SESSION_KEY, null);
					return true;
				}
			});
		} catch (CoreException e) {
			// CoreException - if this method fails. Reasons include:
			// - This resource does not exist.
			// - The visitor failed with this exception.
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * When a new repository is created previous negative cached results should be discarded.
	 */
	public void uncacheAllNegative() {
		noRoot = new String(noRoot); // equals but not ==
	}

	/**
	 * Return any paths other than the given path for which path is canonical of.
	 *
	 * @param path
	 *            The path to query
	 * @return Non null possibly empty list of paths
	 */
	public IPath[] uncanonicalize(IPath path) {
		Set<IPath> candidates = null;
		IPath bestKey = null;

		// Search for key that is a prefix of path
		synchronized (canonicalMap) {
			int matchingSegments = 1;
			SortedMap<IPath, Set<IPath>> map = canonicalMap;
			loop: for (int n = path.segmentCount(); matchingSegments < n && !map.isEmpty(); matchingSegments++) {
				IPath curPrefix = path.removeLastSegments(n - matchingSegments - 1);
				IPath curKey = map.firstKey();

				if (curPrefix.isPrefixOf(curKey)) {
					bestKey = curKey;
					map = map.subMap(curPrefix, map.lastKey());
				} else {
					break loop;
				}
			}

			if (bestKey != null && matchingSegments == bestKey.segmentCount()) {
				candidates = map.get(bestKey);
				assert bestKey.isPrefixOf(path);
			}
		}

		// Build results by switching one prefix for the other
		if (candidates != null && /* redundant */ bestKey != null) {
			IPath pathRel = path.removeFirstSegments(bestKey.segmentCount());
			List<IPath> result = null;

			for (IPath candidate : candidates) {
				// The documentation says the path is canonicalized, but in fact symbolic links
				// aren't normalized.
				candidate = candidate.append(pathRel);

				if (!candidate.equals(path)) {
					if (result == null) {
						result = new ArrayList<IPath>(candidates.size());
					}
					result.add(candidate);
				}
			}

			if (result != null) {
				return result.toArray(new IPath[result.size()]);
			}
		}
		return ResourceUtils.NO_PATHS;
	}

	public static MercurialRootCache getInstance(){
		return MercurialRootCacheHolder.INSTANCE;
	}

	/**
	 * Initialization On Demand Holder idiom, thread-safe and instance will not be created until getInstance is called
	 * in the outer class.
	 */
	private static final class MercurialRootCacheHolder {
		private static final MercurialRootCache INSTANCE = new MercurialRootCache();
		private MercurialRootCacheHolder(){}
	}
}
