/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov         - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Branch specific collection of remote data (changesets) for one remote repository.
 * This data can be queried by project.
 * <p>
 * Additionally, we should think if it may contain project unbound repository data (e.g.
 * files which are not located under any Eclipse project area).
 * @author Andrei
 */
public class RemoteData {

	private static final SortedSet<ChangeSet> EMPTY_SETS = Collections
			.unmodifiableSortedSet(new TreeSet<ChangeSet>());
	private final Map<IProject, ProjectCache> projectMap;
	private final Direction direction;
	private final Map<IPath, Set<ChangeSet>> changesets;
	private final RemoteKey key;

	public RemoteData(RemoteKey key, Direction direction) {
		this(key.getRepo(), key.getRoot(), key.getBranch(), direction, new HashMap<IPath, Set<ChangeSet>>());
	}

	public RemoteData(IHgRepositoryLocation repo, HgRoot root, String branch, Direction direction) {
		this(repo, root, branch, direction, new HashMap<IPath, Set<ChangeSet>>());
	}

	/**
	 * @param changesets this map contains AT LEAST a key corresponding to the hgroot of
	 * this data, and may also contain additional keys for one or more projects under
	 * the given hgroot.
	 * @param branch can be null (means all branches)
	 */
	public RemoteData(IHgRepositoryLocation repo, HgRoot root, String branch, Direction direction,
			Map<IPath, Set<ChangeSet>> changesets) {
		super();
		this.direction = direction;
		this.changesets = changesets;
		key = new RemoteKey(root, repo, branch);
		projectMap = new HashMap<IProject, ProjectCache>();
	}

	public SortedSet<ChangeSet> getChangeSets(IResource resource){
		if(resource == null || changesets.isEmpty()){
			return EMPTY_SETS;
		}
		IProject project = resource.getProject();
		if(project == null){
			return EMPTY_SETS;
		}
		ProjectCache cache = projectMap.get(project);
		if(cache == null) {
			synchronized (projectMap) {
				populateCache(resource.getProject());
			}
			return getChangeSets(resource);
		}
		if (cache.isEmpty()) {
			return EMPTY_SETS;
		}
		if(resource instanceof IProject){
			return cache.getChangesets();
		}
		return cache.getChangesets(resource);
	}

	/**
	 * @return ALL changesets known by the hg root, or empty set, never null
	 */
	public SortedSet<ChangeSet> getChangeSets(){
		if(changesets.isEmpty()){
			return EMPTY_SETS;
		}
		Set<ChangeSet> set = changesets.get(new Path(getRoot().getAbsolutePath()));
		if(set == null || set.isEmpty()) {
			return EMPTY_SETS;
		}
		if(set instanceof SortedSet) {
			return Collections.unmodifiableSortedSet((SortedSet) set);
		}
		TreeSet<ChangeSet> sorted = new TreeSet<ChangeSet>(set);
		return Collections.unmodifiableSortedSet(sorted);
	}

	private void populateCache(IProject project) {
		if(projectMap.containsKey(project)){
			return;
		}
		TreeSet<ChangeSet> psets = new TreeSet<ChangeSet>();
		ProjectCache cache = new ProjectCache(project, getBranch(), psets);
		projectMap.put(project, cache);
		IPath projectPath = ResourceUtils.getPath(project);
		Set<ChangeSet> set = changesets.get(projectPath);
		if(set != null){
			psets.addAll(set);
			return;
		}
		Path rootPath = new Path(getRoot().getAbsolutePath());
		set = changesets.get(rootPath);
		for (ChangeSet changeSet : set) {
			Set<IFile> files = changeSet.getFiles();
			for (IFile file : files) {
				IPath path = ResourceUtils.getPath(file);
				if(!path.isEmpty() && projectPath.isPrefixOf(path)){
					// TODO filter by branch, or it is already filtered?
					// if(Branch.same(branch, changeSet.getBranch()))
					psets.add(changeSet);
					break;
				}
			}
		}
	}

	public boolean isValid(IProject project){
		synchronized (projectMap) {
			return projectMap.containsKey(project);
		}
	}

	public boolean clear(IProject project){
		ProjectCache removed;
		synchronized (projectMap) {
			removed = projectMap.remove(project);
		}
		return removed != null && !removed.isEmpty();
	}

	public boolean clear(){
		boolean changed;
		synchronized (projectMap) {
			changed = !projectMap.isEmpty();
			projectMap.clear();
		}
		return changed;
	}

	/**
	 * @return never null, a list with all projects contained by related hg root directory
	 */
	public Set<IProject> getRelatedProjects(){
		return ResourceUtils.getProjects(getRoot());
	}

	public IHgRepositoryLocation getRepo() {
		return key.getRepo();
	}

	public HgRoot getRoot() {
		return key.getRoot();
	}

	/**
	 * @return specific branch or null if the changesets are not limited by the branch
	 */
	public String getBranch() {
		return key.getBranch();
	}

	public Direction getDirection(){
		return direction;
	}

	public boolean matches(RemoteKey otherKey){
		return key.equals(otherKey);
	}

	public RemoteKey getKey(){
		return key;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RemoteData [");
		if (direction != null) {
			builder.append("direction=");
			builder.append(direction);
			builder.append(", ");
		}
		if (key != null) {
			builder.append("key=");
			builder.append(key);
		}
		builder.append("]");
		return builder.toString();
	}
}
