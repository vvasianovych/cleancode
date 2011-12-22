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
import org.eclipse.jface.viewers.IStructuredSelection;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQDeleteClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Patch;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;

/**
 * @author bastian
 *
 */
public class QDeleteWizard extends HgOperationWizard {

	private static class DeleteOperation extends HgOperation {

		private final boolean isKeep;

		private final IResource resource;

		private final List<Patch> patches;

		private final ChangeSet changeset;

		@SuppressWarnings("unchecked")
		public DeleteOperation(IRunnableContext context, IResource resource, QDeletePage page) {
			super(context);

			IStructuredSelection selection = (IStructuredSelection) page.getPatchViewer().getSelection();

			this.patches = selection.toList();
			this.resource = resource;
			this.isKeep = page.getKeepCheckBox().getSelection();
			this.changeset = page.getSelectedChangeset();
		}

		/**
		 * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
		 */
		@Override
		protected String getActionDescription() {
			return Messages.getString("QDeleteWizard.deleteAction.description"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("QDeleteWizard.deleteAction.beginTask"), 2); //$NON-NLS-1$
			monitor.worked(1);
			monitor.subTask(Messages.getString("QDeleteWizard.subTask.callMercurial")); //$NON-NLS-1$

			try {
				HgQDeleteClient.delete(resource, isKeep, changeset, patches);
				monitor.worked(1);
				monitor.done();
			} catch (HgException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
		}
	}

	private final IResource resource;

	public QDeleteWizard(IResource resource, boolean showRevSelector) {
		super(Messages.getString("QDeleteWizard.title")); //$NON-NLS-1$
		this.resource = resource;
		setNeedsProgressMonitor(true);
		page = new QDeletePage(Messages.getString("QDeleteWizard.pageName"), Messages.getString("QDeleteWizard.pageTitle"), null, //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("QDeleteWizard.pageDescription"), resource, showRevSelector); //$NON-NLS-1$
		initPage(Messages.getString("QDeleteWizard.pageDescription"), page); //$NON-NLS-1$
		addPage(page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#initOperation()
	 */
	@Override
	protected HgOperation initOperation() {
		return new DeleteOperation(getContainer(), resource, (QDeletePage) page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		PatchQueueView.getView().populateTable();
	}
}
