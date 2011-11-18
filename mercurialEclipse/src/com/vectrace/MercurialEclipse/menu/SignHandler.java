/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.SignWizard;

public class SignHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) {
		SignWizard signWizard = new SignWizard(hgRoot);
		WizardDialog dialog = new WizardDialog(getShell(), signWizard);
		dialog.open();
//		if(result == Window.OK) {
			// Andrei: I do not see any reason to update anything after the sign operation.
			// the only change is the changeset info, which is not shown anywhere in Eclipse except
			// the history view, and the history view has a "refresh" button
			// new RefreshJob(Messages.getString("SignHandler.refreshingStatusAndChangesetCache"), project).schedule(); //$NON-NLS-1$
//		}
	}

}
