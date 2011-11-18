/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog.Options;
import com.vectrace.MercurialEclipse.menu.CommitHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public class AddBranchWizard extends HgWizard {
	private final AddBranchPage branchPage;
	private final HgRoot hgRoot;

	private class AddBranchOperation extends HgOperation {

		public AddBranchOperation(IRunnableContext context) {
			super(context);
		}

		@Override
		protected String getActionDescription() {
			return Messages.getString("AddBranchWizard.AddBranchOperation.actionDescription"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {

			boolean commitEnabled = branchPage.isCommitEnabled();
			int workSize = commitEnabled ? 3 : 2;
			monitor.beginTask(Messages.getString("AddBranchWizard.AddBranchOperation.taskName"), workSize); //$NON-NLS-1$
			try {
				String[] dirtyFiles = HgStatusClient.getDirtyFiles(hgRoot);
				if(dirtyFiles.length > 0){
					String message = "There are uncommitted changes in the repository.\n";
					message += "If you continue and commit, the changes will go into the new branch!\n";
					message += "Continue with creating the branch?\n";
					boolean ok = MessageDialog.openConfirm(getShell(), "Add Branch", message);
					if(!ok){
						throw new InterruptedException("Branch creation cancelled because of uncommitted changes!");
					}
				}

				HgBranchClient.addBranch(hgRoot, branchPage.getBranchName(), HgCommitMessageManager
						.getDefaultCommitName(hgRoot), branchPage.isForceEnabled());

				monitor.worked(1);

				HgClients.getConsole().printMessage(result, null);

				Set<IProject> projects = ResourceUtils.getProjects(hgRoot);
				String branch = HgBranchClient.getActiveBranch(hgRoot);
				MercurialTeamProvider.setCurrentBranch(branch, hgRoot);
				if(commitEnabled){
					List<IResource> projectList = new ArrayList<IResource>(projects);
					CommitHandler commitHandler = new CommitHandler();
					Options options = new Options();
					options.defaultCommitMessage = "Starting '" + branch + "' branch";
					options.filesSelectable = false;
					options.showAmend = false;
					options.showCloseBranch = false;
					options.showRevert = false;
					options.allowEmptyCommit = true;
					commitHandler.setOptions(options);
					commitHandler.run(projectList);
					monitor.worked(1);
				}
				for (IProject project : projects) {
					project.touch(monitor);
				}
				monitor.worked(1);
			} catch (CoreException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
			monitor.done();
		}
	}

	public AddBranchWizard(HgRoot hgRoot) {
		super(Messages.getString("AddBranchWizard.windowTitle")); //$NON-NLS-1$
		this.hgRoot = hgRoot;
		setNeedsProgressMonitor(true);
		branchPage = new AddBranchPage(hgRoot, Messages.getString("AddBranchWizard.branchPage.name"), //$NON-NLS-1$
				Messages.getString("AddBranchWizard.branchPage.title"), MercurialEclipsePlugin.getImageDescriptor("wizards/newstream_wizban.gif"), //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("AddBranchWizard.branchPage.description")); //$NON-NLS-1$
		addPage(branchPage);
	}

	@Override
	public boolean performFinish() {
		branchPage.setErrorMessage(null);
		AddBranchOperation op = new AddBranchOperation(getContainer());
		try {
			getContainer().run(false, false, op);
		} catch (Exception e) {
			branchPage.setErrorMessage(e.getLocalizedMessage());
			return false;
		}
		return super.performFinish();
	}
}
