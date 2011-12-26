/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.HgSignClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author Bastian Doetsch
 *
 */
public class SignWizardPage extends HgWizardPage {

	/**
	 * GnuPG key prefix, from the "gpg --list-secret-keys" command output:
	 * "sec   2048R/XXXXXXXX 2009-09-16 [expires: 2010-09-16]"
	 */
	private static final String KEY_PREFIX = "sec";
	private final HgRoot hgRoot;
	private Text userTextField;
	private Combo keyCombo;
	private Button localCheckBox;
	private Button forceCheckBox;
	private Button noCommitCheckBox;
	private ChangesetTable changesetTable;
	private Text messageTextField;
	private Text passTextField;
	private boolean gotGPGkeys;

	public SignWizardPage(String pageName, String title,
			ImageDescriptor titleImage, String description, HgRoot hgRoot) {
		super(pageName, title, titleImage, description);
		this.hgRoot = hgRoot;
	}

	public void createControl(Composite parent) {

		Composite composite = SWTWidgetHelper.createComposite(parent, 2);

		// list view of changesets
		Group changeSetGroup = SWTWidgetHelper.createGroup(
				composite,
				Messages.getString("SignWizardPage.changeSetGroup.title"), //$NON-NLS-1$
				GridData.FILL_BOTH);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 200;
		gridData.minimumHeight = 50;
		changesetTable = new ChangesetTable(changeSetGroup, hgRoot);
		changesetTable.setLayoutData(gridData);
		changesetTable.setEnabled(true);

		SelectionListener listener = new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				ChangeSet cs = changesetTable.getSelection();
				messageTextField.setText(Messages.getString(
						"SignWizardPage.messageTextField.text") //$NON-NLS-1$
						+ " " + cs.toString()); //$NON-NLS-1$
				if (gotGPGkeys) {
					setPageComplete(true);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		changesetTable.addSelectionListener(listener);

		// now the fields for user data
		Group userGroup = SWTWidgetHelper.createGroup(composite,
				Messages.getString("SignWizardPage.userGroup.title")); //$NON-NLS-1$

		SWTWidgetHelper.createLabel(userGroup, Messages.getString("SignWizardPage.userLabel.text")); //$NON-NLS-1$
		userTextField = SWTWidgetHelper.createTextField(userGroup);
		userTextField.setText(HgCommitMessageManager.getDefaultCommitName(hgRoot));

		SWTWidgetHelper.createLabel(userGroup, Messages.getString("SignWizardPage.keyLabel.text")); //$NON-NLS-1$
		keyCombo = SWTWidgetHelper.createCombo(userGroup);

		SWTWidgetHelper.createLabel(userGroup, Messages.getString("SignWizardPage.passphraseLabel.text")); //$NON-NLS-1$
		passTextField = SWTWidgetHelper.createTextField(userGroup);
		// passTextField.setEchoChar('*');
		passTextField
				.setText(Messages.getString("SignWizardPage.passTextField.text")); //$NON-NLS-1$
		passTextField.setEnabled(false);

		// now the options
		Group optionGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("SignWizardPage.optionGroup.title")); //$NON-NLS-1$

		localCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("SignWizardPage.localCheckBox.text")); //$NON-NLS-1$

		forceCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("SignWizardPage.forceCheckBox.text")); //$NON-NLS-1$

		noCommitCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("SignWizardPage.noCommitCheckBox.text")); //$NON-NLS-1$

		SWTWidgetHelper.createLabel(optionGroup, Messages.getString("SignWizardPage.commitLabel.text")); //$NON-NLS-1$
		messageTextField = SWTWidgetHelper.createTextField(optionGroup);
		messageTextField.setText(Messages.getString("SignWizardPage.messageTextField.defaultText")); //$NON-NLS-1$

		populateKeyCombo(keyCombo);
		setControl(composite);
	}

	private void populateKeyCombo(Combo combo) {
		try {
			String keys = HgSignClient.getPrivateKeyList(hgRoot);
			if (keys.indexOf("\n") == -1) { //$NON-NLS-1$
				combo.add(keys);
			} else {
				String[] items = keys.split("\n"); //$NON-NLS-1$
				for (String string : items) {
					string = string.trim();
					if (string.startsWith(KEY_PREFIX)) {
						string = string.substring(KEY_PREFIX.length()).trim();
						if(string.length() > 0) {
							combo.add(string);
						}
					}
				}
			}
			gotGPGkeys = true;
		} catch (HgException e) {
			gotGPGkeys = false;
			combo.add(Messages.getString("SignWizardPage.errorLoadingGpgKeys")); //$NON-NLS-1$
			setPageComplete(false);
			MercurialEclipsePlugin.logError(e);
		}
		if(combo.getItemCount() > 0) {
			combo.setText(combo.getItem(0));
		}
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		ChangeSet cs = changesetTable.getSelection();
		String key = keyCombo.getText();
		if(cs == null){
			setErrorMessage("Please select one changeset");
			return false;
		}
		if(key.trim().length() == 0 || key.indexOf(" ") < 0){
			setErrorMessage("Please select valid key");
			return false;
		}
		key = key.substring(key.indexOf("/") + 1, key.indexOf(" ")); //$NON-NLS-1$ //$NON-NLS-2$
		String msg = messageTextField.getText();
		String user = userTextField.getText();
		String pass = passTextField.getText();
		boolean local = localCheckBox.getSelection();
		boolean force = forceCheckBox.getSelection();
		boolean noCommit = noCommitCheckBox.getSelection();
		try {
			HgSignClient.sign(hgRoot, cs, key, msg,	user, local, force,	noCommit, pass);
		} catch (HgException e) {
			MessageDialog.openInformation(getShell(), Messages.getString("SignWizardPage.errorSigning"), //$NON-NLS-1$
					e.getMessage());
			MercurialEclipsePlugin.logError(e);
			return false;
		}
		return true;
	}
}
