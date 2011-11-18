/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov        - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.io.File;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * A collection of changesets for the given project - branch pair
 * @author Andrei
 */
public class ProjectCache {

	private final IProject project;
	private final SortedSet<ChangeSet> changesets;
	private final String branch;

	/**
	 * @param project non null
	 * @param branch null if no branch info is given (all branches)
	 * @param changesets non null
	 */
	public ProjectCache(IProject project, String branch, SortedSet<ChangeSet> changesets) {
		this.project = project;
		this.branch = branch;
		this.changesets = changesets;
	}

	/**
	 * @return never null
	 */
	public SortedSet<ChangeSet> getChangesets() {
		return changesets;
	}

	/**
	 * @param resource
	 *            non null
	 * @return a subset of known changesets where each changeset has a relationship with given
	 *         resource (e.g. resource was added/modified/removed). In case given resource cannot be
	 *         resolved to a file, or represents the hg root itself, return all known changesets. If
	 *         there is no matching related changeset, an empty set is returned.
	 */
	public SortedSet<ChangeSet> getChangesets(IResource resource) {
		SortedSet<ChangeSet> filtered = new TreeSet<ChangeSet>();
		IPath rootRelative = null;
		mainLoop: for (ChangeSet cs : changesets) {
			if (rootRelative == null) {
				File path = ResourceUtils.getFileHandle(resource);
				if (path == null || path.getPath().length() == 0) {
					return changesets;
				}
				rootRelative = new Path(cs.getHgRoot().toRelative(path));
				if (rootRelative.isEmpty()) {
					// hg root: return everything
					return changesets;
				}
			}
			List<FileStatus> files = cs.getChangedFiles();
			for (FileStatus fs : files) {
				if (rootRelative.equals(fs.getRootRelativePath())) {
					filtered.add(cs);
					continue mainLoop;
				}
			}
		}
		return filtered;
	}

	/**
	 * @return null if no branch info is given (all branches)
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * @return never null
	 */
	public IProject getProject() {
		return project;
	}

	public boolean isEmpty(){
		return changesets.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProjectCache [");
		if (branch != null) {
			builder.append("branch=");
			builder.append(branch);
			builder.append(", ");
		}
		if (project != null) {
			builder.append("project=");
			builder.append(project);
			builder.append(", ");
		}
		if (changesets != null) {
			builder.append("changesets=");
			builder.append(changesets);
		}
		builder.append("]");
		return builder.toString();
	}
}
