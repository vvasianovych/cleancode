/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class UntrackedResourcesFilter extends ViewerFilter {

	private final Map<HgRoot, Set<IPath>> untrackedFiles;
	private final Map<HgRoot, Set<IPath>> untrackedFolders;

	public UntrackedResourcesFilter(Map<HgRoot, Set<IPath>> untrackedFiles,
			Map<HgRoot, Set<IPath>> untrackedFolders) {
		super();
		this.untrackedFiles = untrackedFiles;
		this.untrackedFolders = untrackedFolders;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {

		IResource resource = (IResource) element;
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
		if(hgRoot == null){
			// paranoia
			return false;
		}
		String path = hgRoot.toRelative(ResourceUtils.getFileHandle(resource));

		if(resource.getType() == IResource.FILE) {
			Set<IPath> set = untrackedFiles.get(hgRoot);
			return set != null && matchesPath(path, set);
		} else if(resource.getType() == IResource.FOLDER){
			Set<IPath> set = untrackedFolders.get(hgRoot);
			return set != null && matchesPath(path, set);
		} else {
			// project
			return untrackedFolders.containsKey(hgRoot);
		}
	}

	/**
	 * @param pathStrToTest
	 * @param untrackedPaths known untracked files
	 * @return true if the path to test matches one of known untracked paths
	 */
	private static boolean matchesPath(String pathStrToTest, Set<IPath> untrackedPaths) {
		IPath pathToTest = new Path(pathStrToTest);
		return untrackedPaths.contains(pathToTest);
	}

}
