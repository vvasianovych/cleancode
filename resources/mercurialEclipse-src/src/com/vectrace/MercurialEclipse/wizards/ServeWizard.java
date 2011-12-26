/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class ServeWizard extends HgWizard {
	private HgRoot hgRoot;
	private ServeWizardPage servePage;

	private ServeWizard() {
		super(Messages.getString("ServeWizard.name")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	public ServeWizard(HgRoot hgRoot) {
		this();
		this.hgRoot = hgRoot;
	}

	@Override
	public void addPages() {
		super.addPages();
		servePage = createPage(Messages.getString("ServeWizard.pageName"), Messages.getString("ServeWizard.pageTitle"), Messages //$NON-NLS-1$ //$NON-NLS-2$
				.getString("NewLocationWizard.repoCreationPage.image"), //$NON-NLS-1$
				Messages.getString("ServeWizard.pageDescription")); //$NON-NLS-1$
		addPage(servePage);
	}

	/**
	 * Creates a Page.
	 */
	protected ServeWizardPage createPage(String pageName, String pageTitle,
			String iconPath, String description) {
		this.servePage = new ServeWizardPage(pageName, pageTitle,
				MercurialEclipsePlugin.getImageDescriptor(iconPath), hgRoot);
		initPage(description, servePage);
		return servePage;
	}

	@Override
	public boolean performFinish() {
		boolean finish = servePage.finish(new NullProgressMonitor());
		return finish && super.performFinish();
	}

}
