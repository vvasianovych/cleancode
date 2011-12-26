/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     lordofthepigs            - implementation
 *     Andrei Loskutov			- bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.IniFile;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author lordofthepigs
 *
 */
public class HgSubreposClient extends AbstractClient {

	private static final String HGSUB = ".hgsub";
	private static final String HGDIR = ".hg";

	/**
	 * Returns the list of the subrepositories of the specified Hg repository that are cloned inside the working copy.
	 */
	public static Set<HgRoot> findSubrepositories(HgRoot hgRoot){
		File hgsub = new File(hgRoot, HGSUB);

		if(!hgsub.isFile()){
			return new HashSet<HgRoot>();
		}

		Map<String, String> subrepos;
		try {
			IniFile iniFile = new IniFile(hgsub.getAbsolutePath());
			subrepos = iniFile.getSection(null);
		} catch (FileNotFoundException e) {
			// this shouldn't happen because we checked for existence of the file before, but who knows,
			// bad timing happens...
			MercurialEclipsePlugin.logError(e);
			return new HashSet<HgRoot>();
		}

		if(subrepos == null || subrepos.isEmpty()){
			return new HashSet<HgRoot>();
		}
		Set<HgRoot> result = new HashSet<HgRoot>();
		for(String subReposRootPath : subrepos.keySet()){
			File subReposRootDir = new File(hgRoot, subReposRootPath);
			if(!subReposRootDir.isDirectory()){
				// for some reason the subrepos was not cloned or disappeared, just ignore it
				continue;
			}
			File subRepoHg = new File(subReposRootDir, HGDIR);
			if(subRepoHg.isDirectory()){
				// we are reasonably sure that an HgRoot really exists in subReposRootDir
				try{
					result.add(new HgRoot(subReposRootDir));
				} catch(IOException ioe){
					MercurialEclipsePlugin.logError(ioe);
				}
			}
		}

		return result;
	}

	public static Set<HgRoot> findSubrepositoriesRecursively(HgRoot root){
		Set<HgRoot> found = new HashSet<HgRoot>();
		doFindSubrepositoriesRecursively(root, found, Path.EMPTY);
		return found;
	}

	public static Set<HgRoot> findSubrepositoriesRecursivelyWithin(HgRoot root, IResource container){
		if(container.getType() == IResource.FILE){
			// a file cannot contain a subrepo
			return new HashSet<HgRoot>();
		}

		Set<HgRoot> found = new HashSet<HgRoot>();
		doFindSubrepositoriesRecursively(root, found, ResourceUtils.getPath(container));
		return found;
	}

	/**
	 * recursively finds all the subrepositories under the specified HgRoot and stores all the
	 * discovered subrepos in the specified set. An IPath can optionally be specified, in which
	 * case, all the returned subrepos will be children of that IPath.
	 *
	 * @param containerPath
	 *            non null. Use {@link Path#EMPTY} if the containerPath should be not used
	 */
	private static void doFindSubrepositoriesRecursively(HgRoot root, Set<HgRoot> found, IPath containerPath){
		Set<HgRoot> subs = findSubrepositories(root);
		for(HgRoot sub : subs){
			if(containerPath.isEmpty() || containerPath.isPrefixOf(sub.getIPath())) {
				found.add(sub);
				doFindSubrepositoriesRecursively(sub, found, containerPath);
			}
		}
	}
}
