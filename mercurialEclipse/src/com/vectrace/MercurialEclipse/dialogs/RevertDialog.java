/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.getFillGD;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

public class RevertDialog extends TitleAreaDialog {

	private final List<IResource> resources;
	private CommitFilesChooser selectFilesList;
	private List<IResource> selection;
	private List<IResource> untrackedSelection;
	private ChangesetTable csTable;
	private ChangeSet changeset;

	public static final String FILE_MODIFIED = Messages.getString("CommitDialog.modified"); //$NON-NLS-1$
	public static final String FILE_ADDED = Messages.getString("CommitDialog.added"); //$NON-NLS-1$
	public static final String FILE_REMOVED = Messages.getString("CommitDialog.removed"); //$NON-NLS-1$
	public static final String FILE_UNTRACKED = Messages.getString("CommitDialog.untracked"); //$NON-NLS-1$
	public static final String FILE_DELETED = Messages.getString("CommitDialog.deletedInWorkspace"); //$NON-NLS-1$
	public static final String FILE_CLEAN = Messages.getString("CommitDialog.clean"); //$NON-NLS-1$

	/**
	 * @param parentShell
	 * @param resources non null
	 */
	public RevertDialog(Shell parentShell, List<IResource> resources) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.resources = resources;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings dialogSettings = MercurialEclipsePlugin.getDefault().getDialogSettings();
		String sectionName = getClass().getSimpleName();
		IDialogSettings section = dialogSettings.getSection(sectionName);
		if (section == null) {
			dialogSettings.addNewSection(sectionName);
		}
		return section;
	}

	/**
	 * Create contents of the dialog
	 *
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = SWTWidgetHelper.createComposite(parent, 1);
		GridData gd = getFillGD(400);
		container.setLayoutData(gd);
		super.createDialogArea(parent);
		createFilesList(container);
		createRevertToChangesetWidgets(container);
		getShell().setText(Messages.getString("RevertDialog.window.title")); //$NON-NLS-1$
		setTitle(Messages.getString("RevertDialog.title")); //$NON-NLS-1$
		setMessage(Messages.getString("RevertDialog.message")); //$NON-NLS-1$
		return container;
	}

	private void createRevertToChangesetWidgets(Composite container) {
		Group g = SWTWidgetHelper.createGroup(container, Messages.getString("RevertDialog.revision")); //$NON-NLS-1$
		final Button b = SWTWidgetHelper.createCheckBox(g, Messages.getString("RevertDialog.revertToADifferentChangeset")); //$NON-NLS-1$

		b.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				csTable.setAutoFetch(b.getSelection());
				csTable.setEnabled(b.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		GridData gridData = new GridData(GridData.FILL_BOTH);
		g.setLayoutData(gridData);



		gridData = new GridData(GridData.FILL_BOTH);
		csTable = new ChangesetTable(g, resources.get(0));
		boolean enableChangesetTable = false;
		if(resources.size() == 1){
			enableChangesetTable = MercurialStatusCache.getInstance().isClean(resources.get(0));
		}
		csTable.setAutoFetch(enableChangesetTable);
		csTable.setEnabled(enableChangesetTable);
		csTable.setLayoutData(gridData);
		b.setSelection(enableChangesetTable);
	}

	private void createFilesList(Composite container) {
		selectFilesList = new CommitFilesChooser(container, true, resources, true, true, true);
	}

	@Override
	protected void okPressed() {
		if(resources.size() != 1) {
			selection = selectFilesList.getCheckedResources(FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_REMOVED);
		} else {
			selection = selectFilesList.getCheckedResources(FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_REMOVED, FILE_CLEAN);
		}
		untrackedSelection = selectFilesList.getCheckedResources(FILE_UNTRACKED);
		changeset = csTable.getSelection();

		if(!untrackedSelection.isEmpty()){
			boolean confirm = MessageDialog.openConfirm(getShell(), Messages.getString("RevertDialog.pleaseConfirmDelete"), //$NON-NLS-1$
					Messages.getString("RevertDialog.youHaveSelectedToRevertUntracked") + //$NON-NLS-1$
					Messages.getString("RevertDialog.thisFilesWillNowBeDeleted")); //$NON-NLS-1$
			if(!confirm){
				return;
			}
		}
		super.okPressed();
	}

	public List<IResource> getSelectionForHgRevert() {
		return selection;
	}

	public List<IResource> getUntrackedSelection() {
		return untrackedSelection;
	}

	public ChangesetTable getCsTable() {
		return csTable;
	}

	public ChangeSet getChangeset() {
		return changeset;
	}
}
