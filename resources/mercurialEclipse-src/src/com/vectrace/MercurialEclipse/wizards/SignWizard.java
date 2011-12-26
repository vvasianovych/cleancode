/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian  implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class SignWizard extends Wizard {
	private final SignWizardPage page;

	public SignWizard(HgRoot hgRoot) {
		ImageDescriptor image = MercurialEclipsePlugin
				.getImageDescriptor(Messages.getString("SignWizard.signWizardPage.image")); //$NON-NLS-1$
		page = new SignWizardPage(Messages.getString("SignWizard.signWizardPage.name"), Messages.getString("SignWizard.signWizardPage.title"), image, //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("SignWizard.signWizardPage.description"), hgRoot); //$NON-NLS-1$
		IDialogSettings workbenchSettings = MercurialEclipsePlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("SignWizard"); //$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("SignWizard"); //$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	@Override
	public boolean performFinish() {
		return page.finish(new NullProgressMonitor());
	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(page);
	}

	@Override
	public boolean canFinish() {
		return super.canFinish() && page.isPageComplete();
	}

}
