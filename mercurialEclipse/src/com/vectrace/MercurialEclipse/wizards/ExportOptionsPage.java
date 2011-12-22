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

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
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
public class ExportOptionsPage extends HgWizardPage implements Listener {

	private Button chkGit;
	private Button chkBase;
	private Button chkText;
	private Button chkFunction;
	private Text txtUnified;
	private Button chkUnified;
	private Text txtBase;
	private Button chkNoDate;
	private Button chkIgnoreAllSpace;
	private Button chkIgnoreSpaceChange;
	private Button chkIgnoreBlankLines;

	public ExportOptionsPage() {
		super(Messages.getString("ImportPatchWizard.optionsPageName"), Messages //$NON-NLS-1$
				.getString("ImportPatchWizard.optionsPageTitle"), null); //$NON-NLS-1$
	}

	protected boolean validatePage() {
		return true;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);
		chkGit = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.git")); //$NON-NLS-1$

		chkText = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.text")); //$NON-NLS-1$

		chkBase = createLabelCheckBox(composite, Messages
				.getString("ExportOptionsPage.rev")); //$NON-NLS-1$
		chkBase.addListener(SWT.Selection, this);
		txtBase = SWTWidgetHelper.createTextField(composite);

		chkFunction = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.function")); //$NON-NLS-1$

		chkNoDate = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.noDate")); //$NON-NLS-1$

		chkIgnoreAllSpace = SWTWidgetHelper.createCheckBox(composite, Messages
				.getString("ExportOptionsPage.ignoreAllSpace")); //$NON-NLS-1$
		chkIgnoreSpaceChange = SWTWidgetHelper.createCheckBox(composite,
				Messages.getString("ExportOptionsPage.ignoreSpaceChange")); //$NON-NLS-1$
		chkIgnoreBlankLines = SWTWidgetHelper.createCheckBox(composite,
				Messages.getString("ExportOptionsPage.ignoreBlankLines")); //$NON-NLS-1$

		chkUnified = createLabelCheckBox(composite, Messages
				.getString("ExportOptionsPage.context")); //$NON-NLS-1$
		chkUnified.addListener(SWT.Selection, this);
		txtUnified = SWTWidgetHelper.createTextField(composite);

		setControl(composite);
		validate();
	}

	public void handleEvent(Event event) {
		validate();
	}

	private void validate() {
		txtUnified.setEnabled(chkUnified.getSelection());
		txtBase.setEnabled(chkBase.getSelection());
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
		if (chkBase.getSelection()) {
			list.add("-r " + txtBase.getText()); //$NON-NLS-1$
		}
		if (chkText.getSelection()) {
			list.add("-a"); //$NON-NLS-1$
		}
		if (chkGit.getSelection()) {
			list.add("-g"); //$NON-NLS-1$
		}
		if (chkFunction.getSelection()) {
			list.add("-p"); //$NON-NLS-1$
		}
		if (chkNoDate.getSelection()) {
			list.add("--nodates"); //$NON-NLS-1$
		}
		if (chkIgnoreAllSpace.getSelection()) {
			list.add("-w"); //$NON-NLS-1$
		}
		if (chkIgnoreSpaceChange.getSelection()) {
			list.add("-b"); //$NON-NLS-1$
		}
		if (chkIgnoreBlankLines.getSelection()) {
			list.add("-B"); //$NON-NLS-1$
		}
		if (chkUnified.getSelection()) {
			list.add("-U " + txtUnified.getText()); //$NON-NLS-1$
		}
		return list;
	}

	public static Button createLabelCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		return button;
	}
}
