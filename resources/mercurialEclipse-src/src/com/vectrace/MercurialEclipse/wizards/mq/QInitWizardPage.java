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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.wizards.HgWizardPage;

/**
 * @author bastian
 *
 */
public class QInitWizardPage extends HgWizardPage {

	private IResource resource;
	private Button checkBox;

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public QInitWizardPage(String pageName, String title,
			ImageDescriptor titleImage, String description, IResource resource) {
		super(pageName, title, titleImage, description);
		this.resource = resource;
	}



	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 1);
		Group g = SWTWidgetHelper.createGroup(composite, Messages.getString("QInitWizardPage.group.title")); //$NON-NLS-1$
		this.checkBox = SWTWidgetHelper.createCheckBox(g, Messages.getString("QInitWizardPage.checkBox.nestedRepo")); //$NON-NLS-1$
		setControl(composite);
	}



	/**
	 * @return the resource
	 */
	public IResource getResource() {
		return resource;
	}



	/**
	 * @return the checkBox
	 */
	public Button getCheckBox() {
		return checkBox;
	}

}
