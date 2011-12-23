/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 * Andrei Loskutov - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 *
 * Rebase page. Not applicable if a rebase is in progress.
 */
public class RebasePage extends HgWizardPage {

	private final HgRoot hgRoot;
	private ChangesetTable srcTable;
	private Button sourceRevCheckBox;
	private Button baseRevCheckBox;
	private Button destRevCheckBox;
	private Button collapseRevCheckBox;
	private Button keepCheckBox;
	private Button keepBranchesCheckBox;
	private ChangesetTable destTable;

	public RebasePage(String pageName, String title,
			ImageDescriptor titleImage, String description, HgRoot hgRoot) {
		super(pageName, title, titleImage, description);
		this.hgRoot = hgRoot;
	}

	public void createControl(Composite parent) {
		Composite comp = SWTWidgetHelper.createComposite(parent, 2);

		createSrcWidgets(comp);
		createDestWidgets(comp);
		createOptionsWidgets(comp);

		setControl(comp);
		try {
			if (!MercurialUtilities.isCommandAvailable("rebase", //$NON-NLS-1$
					ResourceProperties.REBASE_AVAILABLE, "hgext.rebase=")) { //$NON-NLS-1$
				setErrorMessage(Messages.getString("RebasePage.error.notAvailable")); //$NON-NLS-1$
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			setErrorMessage(e.getLocalizedMessage());
		}
		setPageComplete(true);
	}

	@Override
	public void setPageComplete(boolean complete) {
		if(complete){
			try {
				if(HgStatusClient.isDirty(hgRoot) && !HgRebaseClient.isRebasing(hgRoot)){
					setErrorMessage("Outstanding uncommitted changes! Rebase is not possible.");
					super.setPageComplete(false);
					return;
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		super.setPageComplete(complete);
	}

	private void createOptionsWidgets(Composite comp) {
		Group optionGroup = SWTWidgetHelper.createGroup(comp, Messages.getString("RebasePage.optionGroup.label"), 2, //$NON-NLS-1$
				GridData.FILL_BOTH);

		collapseRevCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.collapse")); //$NON-NLS-1$
		keepBranchesCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.keepBranches")); //$NON-NLS-1$
		keepCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
				Messages.getString("RebasePage.option.keep")); //$NON-NLS-1$

		if (MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getBoolean(MercurialPreferenceConstants.PREF_DEFAULT_REBASE_KEEP_BRANCHES)) {
			keepBranchesCheckBox.setSelection(true);
		}
	}

	private void createDestWidgets(Composite comp) {
		Group destGroup = SWTWidgetHelper.createGroup(comp,
				Messages.getString("RebasePage.destinationGroup.label"), 2, GridData.FILL_BOTH); //$NON-NLS-1$
		destRevCheckBox = SWTWidgetHelper.createCheckBox(destGroup,
				Messages.getString("RebasePage.destinationCheckbox.label")); //$NON-NLS-1$

		SelectionListener sl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				destTable.setEnabled(destRevCheckBox.getSelection());
			}
		};
		destRevCheckBox.addSelectionListener(sl);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		destTable = new ChangesetTable(destGroup, hgRoot);
		destTable.setLayoutData(gridData);
		destTable.setEnabled(false);
	}

	private void createSrcWidgets(Composite comp) {
		Group srcGroup = SWTWidgetHelper.createGroup(comp,
				Messages.getString("RebasePage.sourceGroup.label"), 2, GridData.FILL_BOTH); //$NON-NLS-1$
		sourceRevCheckBox = SWTWidgetHelper.createCheckBox(srcGroup,
				Messages.getString("RebasePage.source.label")); //$NON-NLS-1$
		baseRevCheckBox = SWTWidgetHelper.createCheckBox(srcGroup,
				Messages.getString("RebasePage.base.label")); //$NON-NLS-1$

		SelectionListener srcSl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				srcTable.setEnabled(sourceRevCheckBox.getSelection()
						|| baseRevCheckBox.getSelection());
				if (sourceRevCheckBox.getSelection()) {
					baseRevCheckBox.setSelection(false);
				}
			}
		};

		SelectionListener baseSl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				srcTable.setEnabled(sourceRevCheckBox.getSelection()
						|| baseRevCheckBox.getSelection());
				if (baseRevCheckBox.getSelection()) {
					sourceRevCheckBox.setSelection(false);
				}
			}
		};

		sourceRevCheckBox.addSelectionListener(srcSl);
		baseRevCheckBox.addSelectionListener(baseSl);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 150;
		gridData.minimumHeight = 50;
		srcTable = new ChangesetTable(srcGroup, hgRoot);
		srcTable.setLayoutData(gridData);
		srcTable.setEnabled(false);
	}

	/**
	 * @return may return -1 if nothing is selected
	 */
	public int getSelectedSrcIndex() {
		ChangeSet selection = srcTable.getSelection();
		return selection == null? -1 : selection.getChangesetIndex();
	}

	public boolean isSourceRevSelected() {
		return sourceRevCheckBox.getSelection();
	}

	public boolean isBaseRevSelected() {
		return baseRevCheckBox.getSelection();
	}

	public boolean isDestRevSelected() {
		return destRevCheckBox.getSelection();
	}

	public boolean isCollapseRevSelected() {
		return collapseRevCheckBox.getSelection();
	}

	public boolean isKeepSelected() {
		return keepCheckBox.getSelection();
	}

	public boolean isKeepBranchesSelected() {
		return keepBranchesCheckBox.getSelection();
	}

	/**
	 * @return may return -1 if nothing is selected
	 */
	public int getSelectedDestIndex() {
		ChangeSet selection = destTable.getSelection();
		return selection == null? -1 : selection.getChangesetIndex();
	}

}
