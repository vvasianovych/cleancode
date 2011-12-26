/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserPanel;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserPanel.Settings;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.UpdateOperation;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.EmptyDataLoader;
import com.vectrace.MercurialEclipse.storage.RootDataLoader;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * The page allows to choose the working directory version (revision/tag/head/branch/bookmark)
 * during the clone repository operation, after cloning te remote repository to the local disk, but
 * before updating working directory to selected version.
 *
 * @author Andrei
 */
public class SelectRevisionPage extends WizardPage {

	private RevisionChooserPanel panel;
	private HgRoot hgRoot;

	public SelectRevisionPage(String pageName) {
		super(pageName);
		setTitle("Select working directory revision");
		setDescription("Select the revision to which the working directory will be updated after clone.\n"
				+ "In case nothing is selected, the working directory will be updated to the latest "
				+ "available changeset.");
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 1);
		setControl(composite);
		DataLoader loader = getDataLoader();
		Settings settings = new Settings();
		settings.highlightDefaultBranch = true;
		panel = new RevisionChooserPanel(composite, loader, settings);
		hookNextButtonListener();
	}

	private DataLoader getDataLoader() {
		DataLoader dataLoader;
		if(hgRoot == null){
			dataLoader = new EmptyDataLoader();
		} else {
			dataLoader = new RootDataLoader(getHgRoot());
		}
		return dataLoader;
	}

	public HgRoot getHgRoot() {
		return hgRoot;
	}

	public String getRevision(){
		if(panel.calculateRevision()){
			return panel.getRevision();
		}
		return null;
	}

	/**
	 * Set the focus on path fields when page becomes visible.
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			IWizardPage previousPage = getPreviousPage();
			if(previousPage instanceof ClonePage){
				ClonePage page = (ClonePage) previousPage;
				File directory = page.getDestinationDirectory();
				setInitialSelection(directory);
			} else if(getWizard() instanceof ImportProjectsFromRepoWizard){
				final ImportProjectsFromRepoWizard impWizard = (ImportProjectsFromRepoWizard) getWizard();
				if(impWizard.getInitialPath() != null) {
					impWizard.getShell().getDisplay().asyncExec(
					new Runnable() {
						public void run() {
							setInitialSelection(impWizard.getInitialPath());
						}
					});
				}
			}
		}
	}

	public void setInitialSelection(File destinationDirectory) {
		if(hgRoot != null && hgRoot.equals(destinationDirectory)){
			return;
		}
		try {
			hgRoot = new HgRoot(destinationDirectory);
			panel.update(getDataLoader());
			setErrorMessage(null);
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
			setErrorMessage("Failed to find hg root at directory " + destinationDirectory);
		}
	}

	private void hookNextButtonListener() {
		IWizardContainer container = getWizard().getContainer();
		if(!(container instanceof WizardDialog)){
			return;
		}
		WizardDialog dialog = (WizardDialog) container;
		dialog.addPageChangingListener(new IPageChangingListener() {

			public void handlePageChanging(PageChangingEvent event) {
				if(!event.doit){
					return;
				}
				if (event.getCurrentPage() == SelectRevisionPage.this
						&& event.getTargetPage() != SelectRevisionPage.this.getPreviousPage()) {
					// Only fire if we're transitioning forward from this page.
					event.doit = nextButtonPressed();
				}
			}
		});
	}

	private boolean nextButtonPressed() {
		setErrorMessage(null);
		if(!panel.calculateRevision()){
			return false;
		}
		String revision = panel.getRevision();
		// TODO validate version string
		boolean ok = validateVersion(revision);
		if(!ok){
			setErrorMessage("Unknown revision: '" + revision + "'!");
			return false;
		}
		try {
			String [] revisionRef = new String []{revision};
			if (hasDataLocally(revisionRef)) {
				// simply forward to the next page
				return true;
			}
			revision = revisionRef[0];
		} catch (HgException e1) {
			MercurialEclipsePlugin.logError(e1);
			setErrorMessage(e1.getLocalizedMessage());
			return false;
		}

		try {
			// run update
			UpdateOperation updateOperation = new UpdateOperation(getContainer(), hgRoot, revision,
					false, false);
			getContainer().run(true, true, updateOperation);
		} catch (InvocationTargetException e) {
			return handle(e);
		} catch (InterruptedException e) {
			// operation cancelled by user
			return false;
		}
		return true;
	}

	private boolean handle(Exception e) {
		if (e.getCause() != null) {
			setErrorMessage(e.getCause().getLocalizedMessage());
		} else {
			setErrorMessage(e.getLocalizedMessage());
		}
		MercurialEclipsePlugin.logError(Messages
				.getString("CloneRepoWizard.updateOperationFailed"), e);
		return false;
	}


	private boolean validateVersion(String revision) {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * @param revisionRef one element array which should contain some version to check.
	 * <b>Note!</b> This array will be misused to put in the requested local changeset index
	 * @return true if the working directory is updated to the selected revision
	 * @throws HgException
	 */
	private boolean hasDataLocally(String[] revisionRef) throws HgException {
		String revision = revisionRef[0];
		boolean noRevSpecified = false;
		if(revision == null) {
			noRevSpecified = true;
		} else {
			revision = revision.trim();
			if(revision.length() == 0){
				noRevSpecified = true;
			}
		}
		ChangeSet expected;
		if(noRevSpecified){
			// take latest available changeset
			// lookup one (latest) revision only
			Map<IPath, Set<ChangeSet>> log = HgLogClient.getRootLog(hgRoot, 1, -1, false);
			Set<ChangeSet> set = log.get(hgRoot.getIPath());
			if(set == null || set.isEmpty()){
				return false;
			}
			expected = set.iterator().next();
		} else {
			expected = HgLogClient.getChangeset(hgRoot, revision);
		}
		if(expected == null){
			return false;
		}
		String changesetId = HgIdentClient.getCurrentChangesetId(hgRoot);
		revisionRef[0] = "" + expected.getChangesetIndex();
		return expected.getChangeset().equals(changesetId);
	}

}
