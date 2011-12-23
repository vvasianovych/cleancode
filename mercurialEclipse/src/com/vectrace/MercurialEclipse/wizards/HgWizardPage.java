/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation to hg
 ******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

/**
 * Common superclass for Hg wizard pages. Provides convenience methods for
 * widget creation.
 */
public abstract class HgWizardPage extends WizardPage {
	protected Properties properties;
	protected IDialogSettings settings;

	/**
	 * HgWizardPage constructor comment.
	 *
	 * @param pageName
	 *            the name of the page
	 */
	public HgWizardPage(String pageName) {
		super(pageName);
	}

	/**
	 * HgWizardPage constructor comment.
	 *
	 * @param pageName
	 *            the name of the page
	 * @param title
	 *            the title of the page
	 * @param titleImage
	 *            the image for the page
	 */
	public HgWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	/**
	 * HgWizardPage constructor comment.
	 *
	 * @param pageName
	 *            the name of the page
	 * @param title
	 *            the title of the page
	 * @param titleImage
	 *            the image for the page
	 * @param description
	 *            the description of the page
	 */
	public HgWizardPage(String pageName, String title,
			ImageDescriptor titleImage, String description) {
		super(pageName, title, titleImage);
		setDescription(description);
	}

	/**
	 * @param monitor
	 * @return
	 */
	public boolean finish(IProgressMonitor monitor) {
		return true;
	}

	/**
	 * Returns the properties for the repository connection
	 *
	 * @return the properties or null
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Sets the properties for the repository connection
	 *
	 * @param properties
	 *            the properties or null
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Override
	public IDialogSettings getDialogSettings() {
		return settings;
	}

	public void setDialogSettings(IDialogSettings settings) {
		this.settings = settings;
	}
}
