/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards.mq;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.commands.HgRemoveClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQNewClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;

/**
 * @author bastian
 *
 */
public class QNewWizard extends HgOperationWizard {

	protected static abstract class FileOperation extends HgOperation {

		protected final HgRoot root;
		protected final String message;
		protected final String user;
		protected final String date;
		protected final List<IResource> resourcesToRemove;
		protected final List<IResource> allResources;
		protected final List<IResource> resourcesToAdd;

		public FileOperation(IRunnableContext context, HgRoot root, QNewWizardPage page) {
			super(context);

			this.root = root;
			this.message = page.getCommitTextDocument().get();
			this.user = page.getUserTextField().getText();
			this.date = page.getDate().getText();

			CommitFilesChooser fileChooser = page.getFileChooser();

			this.resourcesToAdd = fileChooser.getCheckedResources(CommitDialog.FILE_UNTRACKED);
			this.resourcesToRemove = fileChooser.getCheckedResources(CommitDialog.FILE_DELETED);
			this.allResources = fileChooser.getCheckedResources();
		}

		protected void addRemoveFiles(IProgressMonitor pm) throws HgException {
			// add new resources
			pm.subTask("Removing selected untracked resources to repository.");
			HgAddClient.addResources(resourcesToAdd, pm);
			pm.worked(1);

			// remove deleted resources
			pm.subTask("Removing selected deleted resources from repository.");
			HgRemoveClient.removeResources(resourcesToRemove);
			pm.worked(1);
		}
	}

	private static class NewOperation extends FileOperation {

		private final String patchName;

		public NewOperation(IRunnableContext context, HgRoot root, QNewWizardPage page) {
			super(context, root, page);

			this.patchName = page.getPatchNameTextField().getText();
		}

		/**
		 *  @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
		 */
		@Override
		protected String getActionDescription() {
			return Messages.getString("QNewWizard.actionDescription"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("QNewWizard.beginTask"), 4); //$NON-NLS-1$
			monitor.worked(1);

			try {
				addRemoveFiles(monitor);
				monitor.subTask(Messages.getString("QNewWizard.subTask.callMercurial")); //$NON-NLS-1$
				HgQNewClient.createNewPatch(root, message, allResources, user, date, patchName);
				monitor.done();
			} catch (HgException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
		}
	}

	private final HgRoot root;

	public QNewWizard(HgRoot root) {
		super(Messages.getString("QNewWizard.title")); //$NON-NLS-1$
		this.root = root;
		setNeedsProgressMonitor(true);
		page = new QNewWizardPage(
				Messages.getString("QNewWizard.pageName"), Messages.getString("QNewWizard.pageTitle"), //$NON-NLS-1$ //$NON-NLS-2$
				null, null, root, true);
		initPage(Messages.getString("QNewWizard.pageDescription"), //$NON-NLS-1$
				page);
		addPage(page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#initOperation()
	 */
	@Override
	protected HgOperation initOperation() {
		return new NewOperation(getContainer(), root, (QNewWizardPage) page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		super.operationFinished();
		PatchQueueView.getView().populateTable();
		new RefreshWorkspaceStatusJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
	}
}
