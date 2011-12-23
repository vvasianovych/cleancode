/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.wizards.mq.QNewWizard;

public class QNewHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		if (resource != null) {
			HgRoot root = MercurialRootCache.getInstance().hasHgRoot(resource, true);

			if (root != null) {
				openWizard(root, getShell());

				return;
			}
		}

		MessageDialog.openInformation(getShell(), "Couldn't find hg root", "Couldn't find hg root");
	}

	public static void openWizard(HgRoot root, Shell shell) {
		Assert.isNotNull(root);
		QNewWizard wizard = new QNewWizard(root);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}

}
