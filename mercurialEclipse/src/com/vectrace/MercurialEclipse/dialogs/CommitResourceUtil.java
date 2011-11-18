/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Jérôme Nègre              - some fixes
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.Team;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public final class CommitResourceUtil {

	/**
	 * Bit mask of status cache bits for cancidate statuses when committing
	 */
	private static final int COMMIT_CANDIDATE_STATUSES_BITS = MercurialStatusCache.BIT_MISSING
			| MercurialStatusCache.BIT_REMOVED | MercurialStatusCache.BIT_UNKNOWN
			| MercurialStatusCache.BIT_ADDED | MercurialStatusCache.BIT_MODIFIED;

	private CommitResourceUtil() {
		// static utility
	}

	/**
	 * @return never null
	 */
	public static CommitResource[] getCommitResources(List<IResource> inResources) {
		if(inResources.size() == 0){
			return new CommitResource[0];
		}
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(inResources);
		Set<CommitResource> toCommit = new HashSet<CommitResource>();
		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			toCommit.addAll(spliceStatusResult(mapEntry.getKey(), mapEntry.getValue()));
		}
		return toCommit.toArray(new CommitResource[toCommit.size()]);
	}

	/**
	 * Construct the {@link CommitResource} instances.
	 */
	private static List<CommitResource> spliceStatusResult(HgRoot root, List<IResource> resources) {
		MercurialStatusCache cache = MercurialStatusCache.getInstance();
		ArrayList<CommitResource> list = new ArrayList<CommitResource>();

		for (IResource resource : resources) {
			if (resource instanceof IContainer) {
				for (IResource curResource : cache.getResources(COMMIT_CANDIDATE_STATUSES_BITS,
						(IContainer)resource)) {
					processStatusResult(root, cache, list, curResource);
				}
			} else {
				processStatusResult(root, cache, list, resource);
			}
		}

		return list;
	}

	private static void processStatusResult(HgRoot root, MercurialStatusCache cache,
			List<CommitResource> list, IResource resource) {
		if (!(resource instanceof IFile)) {
			return;
		}
		IPath location = ResourceUtils.getPath(resource);
		if(location.isEmpty() || cache.isDirectory(location)) {
			return;
		}
		Integer status = cache.getStatus(resource);
		if (status != null || !Team.isIgnoredHint(resource)) {
			File path = new File(root.toRelative(location.toFile()));
			list.add(new CommitResource(status == null ? MercurialStatusCache.BIT_UNKNOWN : status
					.intValue(), resource, path));
		}
	}

	/**
	 * Filter a list of commit-resources to contain only tracked ones (which are already tracked by Mercurial).
	 */
	public static List<CommitResource> filterForTracked(CommitResource[] commitResources) {
		List<CommitResource> tracked = new ArrayList<CommitResource>();
		for (CommitResource commitResource : commitResources) {
			if (!commitResource.isUnknown()) {
				tracked.add(commitResource);
			}
		}
		return tracked;
	}

	/**
	 * Filter a list of commit-resources to contain only those which are equal with a set of IResources
	 * @param commitResources
	 * @param resources
	 * @return The commit resources
	 */
	public static List<CommitResource> filterForResources(List<CommitResource> commitResources,List<IResource> resources) {
		List<CommitResource> result = new ArrayList<CommitResource>();
		if (resources == null || resources.isEmpty()) {
			return result;
		}
		Set<IResource> resourceSet = new HashSet<IResource>();
		resourceSet.addAll(resources);

		for (CommitResource commitResource : commitResources) {
			IResource res = commitResource.getResource();
			if (res != null && resourceSet.contains(res)) {
				result.add(commitResource);
			}
		}
		return result;
	}

}
