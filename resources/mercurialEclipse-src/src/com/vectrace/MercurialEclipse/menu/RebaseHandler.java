/*******************************************************************************
 * Copyright (c) 2006-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - initial implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.RebaseWizard;

public class RebaseHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) throws CoreException {

		if (HgRebaseClient.isRebasing(hgRoot)) {
			MessageDialog dialog = new MessageDialog(null, Messages
					.getString("RebaseHandler.inProgress"), null, Messages
					.getString("RebaseHandler.inProgressAbortOrContinue"), MessageDialog.QUESTION,
					new String[] { Messages.getString("RebaseHandler.cancel"),
							Messages.getString("RebaseHandler.abort"),
							Messages.getString("RebaseHandler.continue") }, 2);

			switch (dialog.open()) {
			case 1:
				// Shouldn't fail
				HgRebaseClient.abortRebase(hgRoot);
				break;
			case 2:
				ContinueRebaseHandler handler = new ContinueRebaseHandler();
				handler.setShell(getShell());
				handler.run(hgRoot);
				break;
			}
		} else {
			RebaseWizard wizard = new RebaseWizard(hgRoot);
			WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
			wizardDialog.open();
		}
	}

}
