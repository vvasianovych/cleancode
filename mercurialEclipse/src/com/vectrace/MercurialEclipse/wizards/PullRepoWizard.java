/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - saving repository to projec specific repos.
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.util.SortedSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

public class PullRepoWizard extends HgWizard {

	private boolean doUpdate;
	private PullPage pullPage;
	private IncomingPage incomingPage;
	private final HgRoot hgRoot;
	private IHgRepositoryLocation repo;
	private boolean doCleanUpdate;

	public PullRepoWizard(HgRoot hgRoot) {
		super(Messages.getString("PullRepoWizard.title")); //$NON-NLS-1$
		this.hgRoot = hgRoot;
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		pullPage = new PullPage(Messages
				.getString("PullRepoWizard.pullPage.name"), //$NON-NLS-1$
				Messages.getString("PullRepoWizard.pullPage.title"), //$NON-NLS-1$
				Messages.getString("PullRepoWizard.pullPage.description"), //$NON-NLS-1$
				hgRoot, null);

		initPage(pullPage.getDescription(), pullPage);
		addPage(pullPage);

		incomingPage = new IncomingPage(Messages
				.getString("PullRepoWizard.incomingPage.name")); //$NON-NLS-1$
		initPage(incomingPage.getDescription(), incomingPage);
		addPage(incomingPage);
	}

	@Override
	public boolean performFinish() {

		pullPage.finish(new NullProgressMonitor());
		incomingPage.finish(new NullProgressMonitor());
		repo = getLocation();

		doUpdate = pullPage.isUpdateSelected();
		doCleanUpdate = pullPage.isCleanUpdateSelected();
		boolean force = pullPage.isForceSelected();

		ChangeSet cs = null;
		if (incomingPage.isRevisionSelected()) {
			cs = incomingPage.getRevision();
		}

		boolean timeout = pullPage.isTimeoutSelected();
		boolean merge = pullPage.isMergeSelected();
		boolean rebase = pullPage.isRebaseSelected();
		boolean showCommitDialog = pullPage.isShowCommitDialogSelected();
		boolean svn = pullPage.isSvnSelected();
		boolean forest = false;
		File snapFile = null;
		if (pullPage.isShowForest()) {
			forest = pullPage.isForestSelected();
			String snapFileText = pullPage.getSnapFileText();
			if (snapFileText.length() > 0) {
				snapFile = new File(snapFileText);
			}
		}

		File bundleFile = null;
		SortedSet<ChangeSet> changesets = incomingPage.getChangesets();
		if (changesets != null && changesets.size() > 0) {
			bundleFile = changesets.first().getBundleFile();
		}


		try {
			PullOperation pullOperation = new PullOperation(getContainer(),
					doUpdate, doCleanUpdate, hgRoot, force, repo, cs, timeout, merge,
					showCommitDialog, bundleFile, forest, snapFile, rebase, svn);

			getContainer().run(true, false, pullOperation);

			String output = pullOperation.getOutput();

			if (output.length() != 0) {
				HgClients.getConsole().printMessage(output, null);
			}

		} catch (Exception e) {
			Throwable error = e.getCause() == null? e : e.getCause();
			MercurialEclipsePlugin.logError(error);
			MercurialEclipsePlugin.showError(error);
			return false;
		}

		return true;
	}

	private IHgRepositoryLocation getLocation() {
		try {
			return MercurialEclipsePlugin.getRepoManager()
					.fromProperties(hgRoot, pullPage.getProperties());
		} catch (HgException e) {
			MessageDialog.openInformation(getShell(), Messages
					.getString("PullRepoWizard.malformedURL"), e.getMessage()); //$NON-NLS-1$
			MercurialEclipsePlugin.logInfo(e.getMessage(), e);
			return null;
		}
	}
}
