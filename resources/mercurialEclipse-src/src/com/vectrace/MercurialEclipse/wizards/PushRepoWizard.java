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
 *     Bastian Doetsch	         - saving repository to project-specific repos
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSvnClient;
import com.vectrace.MercurialEclipse.commands.extensions.forest.HgFpushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * @author zingo
 *
 */
public class PushRepoWizard extends HgWizard {

	private HgRoot hgRoot;
	private OutgoingPage outgoingPage;

	/**
	 * Optional message to display when first opening the wizard
	 */
	private String message;

	private PushRepoWizard() {
		super(Messages.getString("PushRepoWizard.title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	public PushRepoWizard(HgRoot hgRoot) {
		this();
		this.hgRoot = hgRoot;
	}

	@Override
	public void addPages() {
		super.addPages();
		PushPullPage myPage = new PushRepoPage(
				Messages.getString("PushRepoWizard.pushRepoPage.name"), //$NON-NLS-1$
				Messages.getString("PushRepoWizard.pushRepoPage.title"), null, hgRoot); //$NON-NLS-1$
		initPage(Messages.getString("PushRepoWizard.pushRepoPage.description"), //$NON-NLS-1$
				myPage);
		myPage.setShowCredentials(true);
		page = myPage;
		addPage(page);
		outgoingPage = new OutgoingPage("OutgoingPage"); //$NON-NLS-1$
		initPage(outgoingPage.getDescription(), outgoingPage);
		outgoingPage.setHgRoot(hgRoot);
		addPage(outgoingPage);

		if (message != null) {
			page.setMessage(message, IMessageProvider.WARNING);
		}
	}

	@Override
	public boolean performFinish() {
		super.performFinish();
		Properties props = page.getProperties();
		final IHgRepositoryLocation repo;
		try {
			repo = MercurialEclipsePlugin.getRepoManager().fromProperties(hgRoot, props);
		} catch (HgException e){
			if(!(e.getCause() instanceof URISyntaxException)){
				MercurialEclipsePlugin.logError(e);
			}
			return false;
		}

		final PushPullPage pushRepoPage = (PushPullPage) page;

		final int timeout;
		if (!pushRepoPage.isTimeout()) {
			timeout = Integer.MAX_VALUE;
		} else {
			timeout = HgClients.getTimeOut(MercurialPreferenceConstants.PUSH_TIMEOUT);
		}

		final ChangeSet changeset;
		if (outgoingPage.isRevisionSelected()) {
			changeset = outgoingPage.getRevision();
		} else {
			changeset = null;
		}
		String result = Messages.getString("PushRepoWizard.pushOutput.header"); //$NON-NLS-1$
		final boolean svnEnabled = isSvnEnabled(pushRepoPage);
		final boolean isForest = !svnEnabled && isForestEnabled(pushRepoPage);
		final String snapFileText = pushRepoPage.getSnapFileText();

		class PushOperation extends HgOperation {
			private String output;

			public PushOperation() {
				super(getContainer());
			}

			/**
			 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public void run(IProgressMonitor monitor) throws InvocationTargetException,	InterruptedException {
				monitor.beginTask("Pushing...", IProgressMonitor.UNKNOWN);
				try {
					if (svnEnabled) {
						output = HgSvnClient.push(hgRoot);
					} else if (isForest) {
						File forestRoot = hgRoot.getParentFile();

						File snapFile = null;
						if (snapFileText.length() > 0) {
							snapFile = new File(snapFileText);
						}
						output = HgFpushPullClient.fpush(forestRoot, repo, changeset, timeout, snapFile);
					} else {
						output = HgPushPullClient.push(hgRoot, repo, pushRepoPage.isForce(), changeset, timeout);
					}
				} catch (CoreException e){
					throw new InvocationTargetException(e, e.getMessage());
				} finally {
					monitor.done();
				}
			}

			@Override
			protected String getActionDescription() {
				return "Pushing " + hgRoot.getName() + " ...";
			}

			public String getOutput() {
				return output;
			}
		}

		PushOperation pushOperation = new PushOperation();
		try {
			getContainer().run(true, false, pushOperation);
			result += pushOperation.getOutput();
		} catch (Exception e) {
			Throwable error = e.getCause() == null? e : e.getCause();
			MercurialEclipsePlugin.logError(error);
			MessageDialog.openError(getContainer().getShell(),
					"Error during push", e.getMessage()); //$NON-NLS-1$
			return false;
		}

		try {
			updateAfterPush(result, hgRoot, repo, isForest);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MessageDialog.openError(getContainer().getShell(),
					"Error on refreshing status after push", e.getMessage()); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	private boolean isForestEnabled(PushPullPage pushRepoPage) {
		return pushRepoPage.isShowForest() && pushRepoPage.isForestSelected();
	}

	private boolean isSvnEnabled(PushPullPage pushRepoPage) {
		return pushRepoPage.isShowSvn() && pushRepoPage.isSvnSelected();
	}

	private static void updateAfterPush(String result, HgRoot hgRoot, IHgRepositoryLocation repo, boolean isForest) throws HgException {
		if (result.length() != 0) {
			HgClients.getConsole().printMessage(result, null);
		}

		// It appears good. Stash the repo location.
		MercurialEclipsePlugin.getRepoManager().addRepoLocation(hgRoot, repo);
		if(isForest){
			IncomingChangesetCache.getInstance().clear(repo);
			OutgoingChangesetCache.getInstance().clear(repo);
		} else {
			IncomingChangesetCache.getInstance().clear(hgRoot, true);
			OutgoingChangesetCache.getInstance().clear(hgRoot, true);
		}
	}

	public void setInitialMessage(String message) {
		this.message = message;

		if (page != null) {
			page.setMessage(message, IMessageProvider.WARNING);
		}
	}

}
