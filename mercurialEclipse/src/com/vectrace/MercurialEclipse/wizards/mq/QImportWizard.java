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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.operations.QImportOperation;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;

/**
 * @author bastian
 *
 */
public class QImportWizard extends HgOperationWizard {

	private IResource resource;

	/**
	 * @param windowTitle
	 */
	public QImportWizard(IResource resource) {
		super(Messages.getString("QImportWizard.title")); //$NON-NLS-1$
		this.resource = resource;
		setNeedsProgressMonitor(true);
		page = new QImportWizardPage(
				Messages.getString("QImportWizard.pageName"), //$NON-NLS-1$
				Messages.getString("QImportWizard.page.title"), //$NON-NLS-1$
				Messages.getString("QImportWizard.page.description"), //$NON-NLS-1$
				resource, null);
		initPage(page.getDescription(), page);
		addPage(page);
	}

	@Override
	protected HgOperation initOperation() {
		final QImportWizardPage importPage = (QImportWizardPage) page;

		ChangeSet[] changesets = importPage.getRevisions();
		IPath patchFile = null;
		if (changesets == null) {
			if (importPage.getPatchFile().getText().length()==0) {
				importPage.setErrorMessage(Messages.getString("QImportWizard.page.error.mustSelectChangesetOrFile")); //$NON-NLS-1$
				return null;
			}

			patchFile = new Path(importPage.getPatchFile().getText());
			if (!patchFile.toFile().exists()) {
				importPage.setErrorMessage(Messages.getString("QImportWizard.page.error.patchFileNotExists")); //$NON-NLS-1$
				return null;
			}
		}

		boolean existing = importPage.isExisting();
		boolean force = importPage.getForceCheckBox().getSelection();

		return new QImportOperation(getContainer(), patchFile, changesets, existing, force,
				resource);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		super.operationFinished();
		PatchQueueView.getView().populateTable();
	}

	/**
	 * @return the resource
	 */
	public IResource getResource() {
		return resource;
	}

	/**
	 * @param resource
	 *            the resource to set
	 */
	public void setResource(IResource resource) {
		this.resource = resource;
	}
}
