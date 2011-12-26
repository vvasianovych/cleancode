/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 */
public class AddBranchPage extends HgWizardPage {

	private Button forceCheckBox;
	private Text branchNameTextField;
	private Button commitCheckBox;
	private final HgRoot hgRoot;
	private final Set<String> branchNames;

	public AddBranchPage(HgRoot hgRoot, String pageName, String title,
			ImageDescriptor titleImage, String description) {
		super(pageName, title, titleImage, description);
		this.hgRoot = hgRoot;
		branchNames = new HashSet<String>();
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);
		SWTWidgetHelper.createLabel(composite, Messages.getString("AddBranchPage.branchNameTextField.title")); //$NON-NLS-1$
		try {
			Branch[] branches = HgBranchClient.getBranches(hgRoot);
			for (Branch branch : branches) {
				branchNames.add(branch.getName());
			}
		} catch (HgException e1) {
			MercurialEclipsePlugin.logError(e1);
		}
		branchNameTextField = SWTWidgetHelper.createTextField(composite);
		branchNameTextField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				validateIfBranchExists();
			}
		});
		commitCheckBox = SWTWidgetHelper.createCheckBox(composite, Messages.getString("AddBranchPage.commitCheckBox.title"));         //$NON-NLS-1$
		commitCheckBox.setToolTipText(Messages.getString("AddBranchPage.commitCheckBox.tooltip"));
		commitCheckBox.setSelection(true);
		forceCheckBox = SWTWidgetHelper.createCheckBox(composite, Messages.getString("AddBranchPage.forceCheckBox.title"));         //$NON-NLS-1$
		forceCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				validateIfBranchExists();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		setControl(composite);
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		return super.finish(monitor);
	}

	public boolean isForceEnabled() {
		return forceCheckBox.getSelection();
	}

	public boolean isCommitEnabled() {
		return commitCheckBox.getSelection();
	}

	public String getBranchName() {
		return branchNameTextField.getText();
	}

	/**
	 * @return true if branch does not exists yet
	 */
	public boolean validateIfBranchExists() {
		String text = branchNameTextField.getText();
		if(branchNames.contains(text)){
			if(!forceCheckBox.getSelection()){
				setMessage(null, IMessageProvider.WARNING);
				setErrorMessage("'" + text + "' branch already exists!");
			} else {
				setErrorMessage(null);
				setMessage("'" + text + "' branch shadows existing branch.", IMessageProvider.WARNING);
			}
			return false;
		}
		setMessage(null, IMessageProvider.WARNING);
		setErrorMessage(null);
		return true;
	}

}
