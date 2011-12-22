/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven                    - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Last page of import wizard for import options.
 */
public class ImportOptionsPage extends HgWizardPage implements Listener {

	private Button chkForce;
	private Button chkStrip;
	private Button chkBase;
	private Button chkNoCommit;
	private Button chkExact;
	private Text txtUser;
	private Text txtDate;
	private SourceViewer txtComments;
	private Button chkUser;
	private Button chkDate;
	private Button chkComments;
	private Text txtBase;
	private Text txtStrip;

	public ImportOptionsPage() {
		super(Messages.getString("ImportPatchWizard.optionsPageName"), Messages //$NON-NLS-1$
				.getString("ImportPatchWizard.optionsPageTitle"), null); //$NON-NLS-1$
	}

	protected boolean validatePage() {
		return true;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);
		chkForce = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ImportOptionsPage.force")); //$NON-NLS-1$
		chkStrip = createLabelCheckBox(composite, Messages
				.getString("ImportOptionsPage.strip")); //$NON-NLS-1$
		chkStrip.addListener(SWT.Selection, this);
		txtStrip = SWTWidgetHelper.createTextField(composite);
		chkBase = createLabelCheckBox(composite, Messages
				.getString("ImportOptionsPage.base")); //$NON-NLS-1$
		chkBase.addListener(SWT.Selection, this);
		txtBase = SWTWidgetHelper.createTextField(composite);

		chkNoCommit = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ImportOptionsPage.noCommit")); //$NON-NLS-1$
		chkNoCommit.setSelection(true);
		chkExact = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ImportOptionsPage.exact")); //$NON-NLS-1$

		chkUser = createLabelCheckBox(composite, Messages
				.getString("ImportOptionsPage.user")); //$NON-NLS-1$
		chkUser.addListener(SWT.Selection, this);
		txtUser = SWTWidgetHelper.createTextField(composite);
		chkDate = createLabelCheckBox(composite, Messages
				.getString("ImportOptionsPage.date")); //$NON-NLS-1$
		chkDate.addListener(SWT.Selection, this);
		txtDate = SWTWidgetHelper.createTextField(composite);
		String date = DateFormat.getDateTimeInstance().format(new Date());
		txtDate.setText(date);
		chkComments = createLabelCheckBox(composite, Messages
				.getString("ImportOptionsPage.comments")); //$NON-NLS-1$
		chkComments.addListener(SWT.Selection, this);
		txtComments = SWTWidgetHelper.createTextArea(composite);

		setControl(composite);
		validate();
	}

	public void handleEvent(Event event) {
		validate();
	}

	private void validate() {
		txtUser.setEnabled(chkUser.getSelection());
		txtDate.setEnabled(chkDate.getSelection());
		txtComments.getControl().setEnabled(chkComments.getSelection());
		txtBase.setEnabled(chkBase.getSelection());
		txtStrip.setEnabled(chkStrip.getSelection());
		setErrorMessage(null);
		setPageComplete(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.vectrace.MercurialEclipse.wizards.HgWizardPage#finish(org.eclipse
	 * .core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean finish(IProgressMonitor monitor) {
		// getDialogSettings(); save setttings.
		return super.finish(monitor);
	}

	ArrayList<String> getOptions() {
		ArrayList<String> list = new ArrayList<String>();
		if (chkStrip.getSelection()) {
			list.add("-p " + txtStrip.getText()); //$NON-NLS-1$
		}
		if (chkForce.getSelection()) {
			list.add("-f"); //$NON-NLS-1$
		}
		if (chkBase.getSelection()) {
			list.add("-b " + txtBase.getText()); //$NON-NLS-1$
		}
		if (chkNoCommit.getSelection()) {
			list.add("--no-commit"); //$NON-NLS-1$
		}
		if (chkExact.getSelection()) {
			list.add("--exact"); //$NON-NLS-1$
		}
		if (chkUser.getSelection()) {
			list.add("-u " + txtUser.getText()); //$NON-NLS-1$
		}
		if (chkDate.getSelection()) {
			list.add("-d \"" + txtDate.getText() + '\"'); //$NON-NLS-1$
		}
		if (chkComments.getSelection()) {
			list.add("-m " + txtComments.getDocument().get()); //$NON-NLS-1$
		}
		return list;
	}

	public static Button createLabelCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		return button;
	}
}
