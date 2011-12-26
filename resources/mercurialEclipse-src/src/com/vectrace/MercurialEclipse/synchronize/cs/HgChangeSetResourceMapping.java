/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	   IBM Corporation - initial API and implementation
 *     Andrei Loskutov - adopting to hg
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgChangeSetResourceMapping extends ResourceMapping {

	private final ChangeSet changeSet;
	private final IResource[] resources;
	private FileFromChangeSet file;

	public HgChangeSetResourceMapping(ChangeSet changeSet) {
		this.changeSet = changeSet;
		resources = changeSet.getResources();
	}

	public HgChangeSetResourceMapping(FileFromChangeSet file) {
		this.file = file;
		changeSet = file.getChangeset();
		if(file.getFile() != null) {
			resources = new IResource[]{file.getFile()};
		} else {
			resources = new IResource[0];
		}
	}

	@Override
	public Object getModelObject() {
		return file != null? file : changeSet;
	}

	@Override
	public String getModelProviderId() {
		return HgChangeSetModelProvider.ID;
	}

	@Override
	public IProject[] getProjects() {
		Set<IProject> result = new HashSet<IProject>();
		Set<IFile> files = changeSet.getFiles();
		for (IFile changeSetFile : files) {
			if (changeSetFile != null && changeSetFile.getProject() != null) {
				result.add(changeSetFile.getProject());
			} else {
				// it is possible that files are out of eclipse project scope
				// so resolve project according to hg root.
				final HgRoot root = changeSet.getHgRoot();
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (IProject project : projects) {
					String projectPath = ResourceUtils.getPath(project).toFile().getAbsolutePath();
					String rootPath = root.getAbsolutePath();
					if (projectPath.length() > 0 && projectPath.startsWith(rootPath)) {
						result.add(project);
					}
				}
			}
		}
		return result.toArray(new IProject[result.size()]);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		if (resources.length == 0) {
			return new ResourceTraversal[0];
		}
		return new ResourceTraversal[] {
				new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE)
		};
	}

}
