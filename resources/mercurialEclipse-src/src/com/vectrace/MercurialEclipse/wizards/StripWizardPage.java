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
 *     Ilya Ivanov (Intland) - modofications
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgStripClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 */
public class StripWizardPage extends HgWizardPage {

	private ChangesetTable changesetTable;
	private Button unrelatedCheckBox;
	private ChangeSet stripRevision;
	private final HgRoot hgRoot;
	private Button backupCheckBox;
	private Button stripHeadsCheckBox;
	private boolean unrelated;
	private boolean stripHeads;
	private boolean backup;
	private final ChangeSet changeSet;

	public StripWizardPage(String pageName, String title, ImageDescriptor image,
			HgRoot hgRoot, ChangeSet changeSet) {
		super(pageName, title, image);
		this.hgRoot = hgRoot;
		this.changeSet = changeSet;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);

		// list view of changesets
		Group changeSetGroup = SWTWidgetHelper.createGroup(composite,
				Messages.getString("StripWizardPage.changeSetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

		changesetTable = new ChangesetTable(changeSetGroup, hgRoot);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 200;
		gridData.minimumHeight = 50;
		changesetTable.setLayoutData(gridData);

		SelectionListener listener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				stripRevision = changesetTable.getSelection();
				setPageComplete(true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

		};

		changesetTable.addSelectionListener(listener);
		changesetTable.setEnabled(true);

		changesetTable.setSelection(changeSet);

		// now the options
		Group optionGroup = SWTWidgetHelper.createGroup(composite, Messages.getString("StripWizardPage.optionsGroup.title")); //$NON-NLS-1$

		// backup
		unrelatedCheckBox = SWTWidgetHelper.createCheckBox(
				optionGroup,
				Messages.getString("StripWizardPage.unrelatedCheckBox.title")); //$NON-NLS-1$
		unrelatedCheckBox.setSelection(true);
		backupCheckBox = SWTWidgetHelper.createCheckBox(optionGroup, Messages.getString("StripWizardPage.backupCheckBox.title")); //$NON-NLS-1$
		backupCheckBox.setSelection(true);
		stripHeadsCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("StripWizardPage.stripHeadsCheckBox.title")); //$NON-NLS-1$

		setControl(composite);
		setPageComplete(true);
	}

	@Override
	public void setPageComplete(boolean complete) {
		if(complete){
			try {
				if(HgStatusClient.isDirty(hgRoot)){
					setErrorMessage("Outstanding uncommitted changes! Strip is not possible.");
					super.setPageComplete(false);
					return;
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		super.setPageComplete(complete);
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		super.finish(monitor);
		stripRevision = changesetTable.getSelection();
		unrelated = unrelatedCheckBox.getSelection();
		stripHeads = stripHeadsCheckBox.getSelection();
		backup = backupCheckBox.getSelection();
		try {
			String result = HgStripClient.strip(hgRoot, unrelated, backup, stripHeads, stripRevision);
			HgClients.getConsole().printMessage(result, null);
		} catch (HgException e) {
			MessageDialog.openError(getShell(), Messages.getString("StripWizardPage.errorCallingStrip"), e //$NON-NLS-1$
					.getMessage());
			MercurialEclipsePlugin.logError(e);
			return false;
		}
		return true;
	}

}
