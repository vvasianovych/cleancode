/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov        - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse.wizards;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgPath;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This class implements the import wizard extension and the new wizard
 * extension.
 */
public class ImportProjectsFromRepoWizard extends HgWizard implements IImportWizard, INewWizard {
	private ProjectsImportPage importPage;
	private HgPath initialLocation;


	public ImportProjectsFromRepoWizard() {
		super("Import Projects from Mercurial repository");
	}

	@Override
	public boolean performFinish() {
		return importPage.createProjects();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Projects from Mercurial repository");
		setNeedsProgressMonitor(true);

		if(!selection.isEmpty()){
			Object firstElement = selection.getFirstElement();
			HgPath path = MercurialEclipsePlugin.getAdapter(firstElement, HgPath.class);
			if(path != null){
				setInitialData(path);
			} else {
				IResource resource = MercurialEclipsePlugin.getAdapter(firstElement, IResource.class);
				IPath iPath = ResourceUtils.getPath(resource);
				if(iPath != null && !iPath.isEmpty()) {
					try {
						setInitialData(new HgPath(iPath.toOSString()));
					} catch (IOException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
			}
		}

		importPage = new ProjectsImportPage("ProjectsImportPage");
		importPage.setDestinationSelectionEnabled(true);
		addPage(importPage);
	}

	public void setInitialData(HgPath path) {
		initialLocation = path;
	}

	public HgPath getInitialPath() {
		return initialLocation;
	}
}
