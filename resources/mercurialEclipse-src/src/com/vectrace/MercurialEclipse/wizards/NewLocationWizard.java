/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Andrei Loskutov - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.util.Properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgInitClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgPath;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.repository.RepositoriesView;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

/**
 * Wizard to add a new location. Uses ConfigurationWizardMainPage for entering
 * informations about Hg repository location
 */
public class NewLocationWizard extends HgWizard implements INewWizard {

	private IHgRepositoryLocation repository;

	public NewLocationWizard() {
		super(Messages.getString("NewLocationWizard.name")); //$NON-NLS-1$
	}

	@Override
	public void addPages() {
		page = createPage(Messages.getString("NewLocationWizard.repoCreationPage.description")); //$NON-NLS-1$
		addPage(page);
	}

	/**
	 * @see IWizard#performFinish
	 */
	@Override
	public boolean performFinish() {
		super.performFinish();
		CreateRepoPage createRepoPage = (CreateRepoPage) page;
		File localRepo = createRepoPage.getLocalRepo();
		if(localRepo != null && !HgPath.isHgRoot(localRepo) && createRepoPage.shouldInitRepo()){
			try {
				HgInitClient.init(localRepo);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
				page.setErrorMessage(e.getMessage());
				return false;
			}
		}
		Properties props = page.getProperties();
		HgRepositoryLocationManager manager = MercurialEclipsePlugin.getRepoManager();
		try {
			repository = manager.createRepository(props);
		} catch (HgException ex) {
			MercurialEclipsePlugin.logError(ex);
			return false;
		}
		try {
			RepositoriesView view = getRepoView();
			view.refreshViewer(repository, true);
		} catch (PartInitException e) {
			MercurialEclipsePlugin.logError(e);
		}
		if (createRepoPage.shouldInitRepo() && !repository.isLocal()
				&& repository.getLocation().startsWith("ssh:")) {
			boolean confirm = MessageDialog.openConfirm(getShell(), "Hg init", "Do you really want to run hg init on remote server?");
			if(!confirm){
				return true;
			}
			try {
				HgInitClient.init(repository);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
				page.setErrorMessage(e.getMessage());
				return false;
			}
		}
		return true;
	}

	private RepositoriesView getRepoView() throws PartInitException {
		IWorkbenchPage activePage = MercurialEclipsePlugin.getActivePage();
		return (RepositoriesView) activePage.showView(RepositoriesView.VIEW_ID);
	}

	/**
	 * Creates a ConfigurationWizardPage.
	 */
	protected HgWizardPage createPage(String description) {
		page = new CreateRepoPage();
		initPage(description, page);
		return page;
	}

	public IHgRepositoryLocation getRepository() {
		return repository;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// noop
	}
}
