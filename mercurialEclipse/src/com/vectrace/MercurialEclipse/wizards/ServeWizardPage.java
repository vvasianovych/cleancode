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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.commands.HgServeClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class ServeWizardPage extends HgWizardPage {

	private final HgRoot hgroot;
	private Text nameTextField;
	private Text prefixTextField;
	private Button defaultCheckBox;
	private Text portTextField;
	private Button ipv6CheckBox;
	private Button stdioCheckBox;
	private Text webdirConfTextField;

	public ServeWizardPage(String pageName, String title,
			ImageDescriptor image, HgRoot hgRoot) {
		super(pageName, title, image);
		this.hgroot = hgRoot;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 3);
		// server settings

		final Group defaultsGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("ServeWizardPage.defaultsGroup.title")); //$NON-NLS-1$
		final Group settingsGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("ServeWizardPage.settingsGroup.title")); //$NON-NLS-1$
		settingsGroup.setEnabled(false);

		this.defaultCheckBox = SWTWidgetHelper.createCheckBox(defaultsGroup, Messages.getString("ServeWizardPage.defaultCheckBox.title")); //$NON-NLS-1$
		this.defaultCheckBox.setSelection(true);


		final Label portLabel = SWTWidgetHelper.createLabel(settingsGroup,
				Messages.getString("ServeWizardPage.portLabel.title")); //$NON-NLS-1$
		portLabel.setEnabled(false);
		portTextField = SWTWidgetHelper.createTextField(settingsGroup);
		portTextField.setEnabled(false);
		portTextField.setText(Messages.getString("ServeWizardPage.portTextField.defaultValue")); //$NON-NLS-1$
		portTextField.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				String string = e.text;
				char[] chars = new char[string.length()];
				string.getChars(0, chars.length, chars, 0);
				for (int i = 0; i < chars.length; i++) {
					if (!('0' <= chars[i] && chars[i] <= '9')) {
						e.doit = false;
						return;
					}
				}
			}
		});

		final Label nameLabel = SWTWidgetHelper.createLabel(settingsGroup,
				Messages.getString("ServeWizardPage.nameLabel.title")); //$NON-NLS-1$
		nameLabel.setEnabled(false);
		this.nameTextField = SWTWidgetHelper.createTextField(settingsGroup);
		this.nameTextField.setEnabled(false);

		final Label prefixLabel = SWTWidgetHelper.createLabel(settingsGroup,
				Messages.getString("ServeWizardPage.prefixLabel.title")); //$NON-NLS-1$
		prefixLabel.setEnabled(false);
		this.prefixTextField = SWTWidgetHelper.createTextField(settingsGroup);
		this.prefixTextField.setEnabled(false);

		final Label webdirLabel = SWTWidgetHelper.createLabel(settingsGroup,
				Messages.getString("ServeWizardPage.webdirLabel.title")); //$NON-NLS-1$
		webdirLabel.setEnabled(false);
		this.webdirConfTextField = SWTWidgetHelper.createTextField(settingsGroup);
		this.webdirConfTextField.setEnabled(false);

		this.stdioCheckBox = SWTWidgetHelper.createCheckBox(settingsGroup, Messages.getString("ServeWizardPage.stdioCheckBox.title")); //$NON-NLS-1$
		this.stdioCheckBox.setEnabled(false);

		this.ipv6CheckBox = SWTWidgetHelper.createCheckBox(settingsGroup,
				Messages.getString("ServeWizardPage.ipv6CheckBox.title")); //$NON-NLS-1$
		this.ipv6CheckBox.setEnabled(false);

		SelectionListener defaultCheckBoxListener = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				settingsGroup.setEnabled(!defaultCheckBox.getSelection());
				portLabel.setEnabled(!defaultCheckBox.getSelection());
				portTextField.setEnabled(!defaultCheckBox.getSelection());
				nameLabel.setEnabled(!defaultCheckBox.getSelection());
				nameTextField.setEnabled(!defaultCheckBox.getSelection());
				prefixLabel.setEnabled(!defaultCheckBox.getSelection());
				prefixTextField.setEnabled(!defaultCheckBox.getSelection());
				webdirLabel.setEnabled(!defaultCheckBox.getSelection());
				webdirConfTextField.setEnabled(!defaultCheckBox.getSelection());

				stdioCheckBox.setEnabled(!defaultCheckBox.getSelection());
				ipv6CheckBox.setEnabled(!defaultCheckBox.getSelection());
			}
		};

		this.defaultCheckBox.addSelectionListener(defaultCheckBoxListener);
		setControl(composite);
		setPageComplete(true);
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		super.finish(monitor);
		// call hg
		try {
			boolean serveStarted = HgServeClient.serve(hgroot, Integer.parseInt(portTextField
					.getText()), prefixTextField.getText(), nameTextField
					.getText(), webdirConfTextField.getText(), stdioCheckBox
					.getSelection(), ipv6CheckBox.getSelection());
			if(!serveStarted){
				setErrorMessage("Hg server could not be started. Probably port "
						+ portTextField.getText() + " is already in use");
			}
			return serveStarted;
		} catch (NumberFormatException e) {
			setErrorMessage("Invalid port text");
			return false;
		}
	}

}
