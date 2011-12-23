/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards.mq;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.wizards.HgWizardPage;

public class QImportWizardPage extends HgWizardPage {

	private Button revCheckBox;
	private ChangesetTable changesetTable;
	private ChangeSet[] revisions;
	private IResource resource;
	private Text patchFile;
	private Button browseButton;
	private Button forceCheckBox;
	private Label patchFileLabel;
	private Group patchNameGroup;
	private boolean existing;

	public QImportWizardPage(String pageName, String title, String description,
			IResource resource, ImageDescriptor titleImage) {
		super(pageName, title, titleImage, description);
		this.resource = resource;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 3);
		createPatchNameGroup(composite);
		createOptionGroup(composite);
		createRevisionTable(composite);
		setPageComplete(true);
		setControl(composite);
	}

	private void createOptionGroup(Composite composite) {
		Group g = SWTWidgetHelper.createGroup(composite, Messages
				.getString("QImportWizardPage.optionsGroup.title")); //$NON-NLS-1$
		this.forceCheckBox = SWTWidgetHelper.createCheckBox(g, Messages
				.getString("QImportWizardPage.forceCheckBox.title")); //$NON-NLS-1$
	}

	private void createPatchNameGroup(Composite composite) {
		this.patchNameGroup = SWTWidgetHelper
				.createGroup(
						composite,
						Messages
								.getString("QImportWizardPage.patchNameGroup.title"), 3, GridData.FILL_HORIZONTAL); //$NON-NLS-1$
		this.patchFileLabel = SWTWidgetHelper.createLabel(patchNameGroup,
				Messages.getString("QImportWizardPage.patchFileLabel.title")); //$NON-NLS-1$
		this.patchFile = SWTWidgetHelper.createTextField(patchNameGroup);

		this.patchFile.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (patchFile.getText().length() > 0) {
					try {
						File file = new File(patchFile.getText());
						checkExisting(file);
					} catch (Exception e1) {
						setErrorMessage(e1.getCause().getLocalizedMessage());
					}
				}
			}
		});

		this.browseButton = SWTWidgetHelper.createPushButton(patchNameGroup,
				Messages.getString("QImportWizardPage.browseButton.title"), 1); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					FileDialog dialog = new FileDialog(getShell());
					dialog
							.setText(Messages
									.getString("QImportWizardPage.browseFileDialog.title")); //$NON-NLS-1$
					String fileName = dialog.open();
					if (fileName != null) {
						File file = new File(fileName);

						checkExisting(file);

						patchFile.setText(file.getCanonicalPath());
					}
				} catch (Exception e1) {
					String msg = e1.getLocalizedMessage();
					if (e1.getCause() != null) {
						msg = e1.getCause().getLocalizedMessage();
						setErrorMessage(msg);
						MercurialEclipsePlugin.logError(e1);
					}
				}

			}
		});
	}

	private void checkExisting(File file) throws IOException, HgException {
		setMessage(null);
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
		if(hgRoot == null) {
			throw new HgException("No hg root found for: " + resource);
		}
		File patchDir = new File(hgRoot, ".hg" + File.separator + "patches"); //$NON-NLS-1$ //$NON-NLS-2$
		File[] patches = patchDir.listFiles();
		if (patches != null) {
			for (File patch : patches) {
				if (patch.getName().equals(file.getName())
						|| patch.getCanonicalPath().equals(file.getCanonicalPath())) {
					setMessage(Messages
							.getString("QImportWizardPage.message.Existing")); //$NON-NLS-1$
					existing = true;
				}
			}
		}
	}

	private void createRevisionTable(Composite composite) {
		Group revGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("QImportWizardPage.revGroup.title"), //$NON-NLS-1$
				GridData.FILL_BOTH);
		this.revCheckBox = SWTWidgetHelper.createCheckBox(revGroup, Messages
				.getString("QImportWizardPage.revCheckBox.title")); //$NON-NLS-1$

		Listener revCheckBoxListener = new Listener() {
			public void handleEvent(Event event) {
				if (revCheckBox.getSelection()) {
					if (changesetTable.getChangesets() == null
							|| changesetTable.getChangesets().length == 0) {
						setErrorMessage(null);
					}
				}
				// en-/disable patch file text field
				changesetTable.setEnabled(revCheckBox.getSelection());
				patchFile.setEnabled(!revCheckBox.getSelection());
				browseButton.setEnabled(!revCheckBox.getSelection());
				patchFileLabel.setEnabled(!revCheckBox.getSelection());
				patchNameGroup.setEnabled(!revCheckBox.getSelection());
			}
		};

		this.revCheckBox.addListener(SWT.Selection, revCheckBoxListener);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		this.changesetTable = new ChangesetTable(revGroup,
				SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
						| SWT.H_SCROLL, resource, true);
		this.changesetTable.setLayoutData(gridData);
		this.changesetTable.setEnabled(false);

		SelectionListener listener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(true);
				revisions = changesetTable.getSelections();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		this.changesetTable.addSelectionListener(listener);
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		return super.finish(monitor);
	}

	public ChangeSet[] getRevisions() {
		return revisions;
	}

	public IResource getResource() {
		return resource;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}

	public Button getRevCheckBox() {
		return revCheckBox;
	}

	public Text getPatchFile() {
		return patchFile;
	}

	public Button getForceCheckBox() {
		return forceCheckBox;
	}

	public boolean isExisting() {
		return existing;
	}

}
