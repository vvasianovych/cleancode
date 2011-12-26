/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	   - implementation
 * Philip Graf - Fixed bugs which FindBugs found
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.core.text.TextSearchRequestor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgGrepClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchVisitor {

	private TextSearchRequestor requestor;
	private Pattern pattern;

	/**
	 *
	 */
	public MercurialTextSearchVisitor() {
	}

	/**
	 * @param requestor
	 * @param searchPattern
	 */
	public MercurialTextSearchVisitor(TextSearchRequestor requestor, Pattern searchPattern) {
		this.requestor = requestor;
		this.pattern = searchPattern;
	}

	/**
	 * @param scope
	 * @param monitor
	 * @return
	 */
	public IStatus search(MercurialTextSearchScope scope, IProgressMonitor monitor) {
		IResource[] scopeRoots = scope.getRoots();
		boolean all = scope.isAll();

		if (scopeRoots.length == 1 && scopeRoots[0].getParent() == null) {
			// this is workspace root
			IWorkspace root = ResourcesPlugin.getWorkspace();
			IProject[] projects = root.getRoot().getProjects();

			scopeRoots = projects;
		}

		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(Arrays
				.asList(scopeRoots));
		String searchString = pattern.pattern();
		monitor.beginTask("Searching for " + searchString + " with Mercurial",
				scopeRoots.length * 5);
		for (Entry<HgRoot, List<IResource>> entry : resourcesByRoot.entrySet()) {
			HgRoot root = entry.getKey();
			monitor.subTask("Searching in respository " + root.getName());
			monitor.worked(1);
			try {
				IStatus result = search(root, entry.getValue(), monitor, all);
				if (!result.isOK()) {
					return result;
				}
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
						e.getLocalizedMessage(), e);
			}
		}
		return new Status(IStatus.OK, MercurialEclipsePlugin.ID,
				"Mercurial search completed successfully.");
	}

	/**
	 * @param root
	 * @param all
	 * @throws CoreException
	 */
	private IStatus search(HgRoot root, List<IResource> resources, IProgressMonitor monitor, boolean all)
			throws CoreException {
		try {
			requestor.beginReporting();
			monitor.subTask("Calling Mercurial grep command...");
			List<MercurialTextSearchMatchAccess> result = HgGrepClient.grep(root,
					pattern.pattern(), resources, all, monitor);
			monitor.worked(1);
			monitor.subTask("Processing Mercurial grep results...");
			for (MercurialTextSearchMatchAccess sr : result) {
				if (monitor.isCanceled()){
					break;
				}
				if (sr.getFile() != null) {
					monitor.subTask("Found match in: " + sr.getFile().getName());
					requestor.acceptFile(sr.getFile());
					monitor.worked(1);
					requestor.acceptPatternMatch(sr);
					monitor.worked(1);
				}
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, e.getLocalizedMessage(), e);
		}
		requestor.endReporting();
		return new Status(IStatus.OK, MercurialEclipsePlugin.ID,
				"Mercurial search completed successfully.");
	}

	/**
	 * @param scope
	 * @param monitor
	 * @return
	 */
	public IStatus search(IFile[] scope, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

}
