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
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.ImportPatchOperation;

public class ImportPatchWizard extends HgOperationWizard {

	private final ImportPatchPage sourcePage;
	private final ImportOptionsPage optionsPage;

	final HgRoot hgRoot;

	public ImportPatchWizard(HgRoot hgRoot) {
		super(Messages.getString("ImportPatchWizard.WizardTitle"));
		setNeedsProgressMonitor(true);
		this.hgRoot = hgRoot;

		sourcePage = new ImportPatchPage(hgRoot);
		addPage(sourcePage);
		initPage(Messages.getString("ImportPatchWizard.pageDescription"), sourcePage);

		optionsPage = new ImportOptionsPage();
		addPage(optionsPage);
		initPage(Messages.getString("ImportPatchWizard.optionsPageDescription"), optionsPage);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#initOperation()
	 */
	@Override
	protected HgOperation initOperation() {
		sourcePage.finish(null);
		return new ImportPatchOperation(getContainer(), hgRoot, sourcePage.getLocation(),
				optionsPage.getOptions());
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationSucceeded(HgOperation)
	 */
	@Override
	protected boolean operationSucceeded(HgOperation operation) throws HgException {
		if (((ImportPatchOperation) operation).isConflict()) {
			MessageDialog.openInformation(getShell(),
					Messages.getString("ImportPatchWizard.WizardTitle"),
					Messages.getString("ImportPatchWizard.conflict") + "\n" +  operation.getResult());
		}

		return super.operationSucceeded(operation);
	}
}
