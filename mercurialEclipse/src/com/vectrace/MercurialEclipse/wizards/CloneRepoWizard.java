/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch	         - saving repository to project-specific repos
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

/**
 * This class implements the import wizard extension and the new wizard
 * extension.
 */
public class CloneRepoWizard extends HgWizard implements IImportWizard, INewWizard {
	private final ClonePage clonePage;
	private final ProjectsImportPage importPage;
	private IHgRepositoryLocation defaultLocation;
	private final SelectRevisionPage selectRevisionPage;


	public CloneRepoWizard() {
		super(Messages.getString("CloneRepoWizard.title")); //$NON-NLS-1$
		clonePage = new ClonePage(null, Messages
				.getString("CloneRepoWizard.pageName"), //$NON-NLS-1$
				Messages.getString("CloneRepoWizard.pageTitle"), null); //$NON-NLS-1$
		clonePage.setDescription(Messages
				.getString("CloneRepoWizard.pageDescription")); //$NON-NLS-1$
		initPage(clonePage.getDescription(), clonePage);
		setNeedsProgressMonitor(true);

		selectRevisionPage = new SelectRevisionPage("SelectRevisionPage");
		importPage = new ProjectsImportPage("ProjectsImportPage");

		addPage(clonePage);
		addPage(selectRevisionPage);
		addPage(importPage);
	}

	@Override
	public boolean performFinish() {
		return importPage.createProjects();
	}

	@Override
	public boolean performCancel() {
		clonePage.performCleanup();
		return super.performCancel();
	}

	@Override
	public IWizardPage getStartingPage() {
		return clonePage;
	}

	public IHgRepositoryLocation getRepository() {
		return clonePage.getLastUsedRepository();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.getString("CloneRepoWizard.title")); //$NON-NLS-1$

		if(!selection.isEmpty()){
			Object firstElement = selection.getFirstElement();
			if(firstElement instanceof IHgRepositoryLocation){
				IHgRepositoryLocation repo = (IHgRepositoryLocation) firstElement;
				setDefaultLocation(repo);
			}
		}
		if(getDefaultLocation() != null) {
			clonePage.setInitialRepo(getDefaultLocation());
		}
	}

	@Override
	public boolean canFinish() {
		return getContainer() != null && getContainer().getCurrentPage() instanceof ProjectsImportPage && super.canFinish();
	}

	public void setDefaultLocation(IHgRepositoryLocation defaultLocation) {
		this.defaultLocation = defaultLocation;
	}

	public IHgRepositoryLocation getDefaultLocation() {
		return defaultLocation;
	}
}
