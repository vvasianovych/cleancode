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
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.wizards.mq.QImportWizard;

/**
 * @author bastian
 *
 */
public class QImportHandler extends SingleResourceHandler {

	/* (non-Javadoc)
	 * @see com.vectrace.MercurialEclipse.menu.SingleResourceHandler#run(org.eclipse.core.resources.IResource)
	 */
	@Override
	protected void run(IResource resource) throws Exception {
		openWizard(resource, getShell());
	}

	/**
	 * @param resource
	 * @param shell
	 */
	public static void openWizard(IResource resource, Shell shell) {
		QImportWizard wizard = new QImportWizard(resource);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}

}
