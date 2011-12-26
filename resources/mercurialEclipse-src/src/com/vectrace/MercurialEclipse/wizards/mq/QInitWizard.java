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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQInitClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;

/**
 * @author bastian
 *
 */
public class QInitWizard extends HgOperationWizard {

	private static class InitOperation extends HgOperation {

		private final boolean isRepository;
		private final IResource resource;

		/**
		 * @param context
		 */
		public InitOperation(IRunnableContext context, IResource resource, QInitWizardPage page) {
			super(context);

			this.isRepository = page.getCheckBox().getSelection();
			this.resource = resource;
		}

		/**
		 * @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
		 */
		@Override
		protected String getActionDescription() {
			return Messages.getString("QInitWizard.InitAction.description"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("QInitWizard.beginTask"), 2); //$NON-NLS-1$
			monitor.worked(1);
			monitor.subTask(Messages.getString("QInitWizard.subTask.callMercurial")); //$NON-NLS-1$

			try {
				HgQInitClient.init(resource, isRepository);
				monitor.worked(1);
				monitor.done();
			} catch (HgException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
		}

	}

	private final IResource resource;

	/**
	 * @param windowTitle
	 */
	public QInitWizard(IResource resource) {
		super(Messages.getString("QInitWizard.title")); //$NON-NLS-1$
		this.resource = resource;
		setNeedsProgressMonitor(true);
		page = new QInitWizardPage(
				Messages.getString("QInitWizard.pageName"), //$NON-NLS-1$
				Messages.getString("QInitWizard.pageTitle"), //$NON-NLS-1$
				null,
				null,
				resource);
		initPage(Messages.getString("QInitWizard.pageDescription"), page); //$NON-NLS-1$
		addPage(page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#initOperation()
	 */
	@Override
	protected HgOperation initOperation() {
		return new InitOperation(getContainer(), resource, (QInitWizardPage) page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		super.operationFinished();
		PatchQueueView.getView().populateTable();
	}
}
