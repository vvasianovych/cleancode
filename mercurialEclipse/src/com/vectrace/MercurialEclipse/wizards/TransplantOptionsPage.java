/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch				- implementation
 * 		Andrei Loskutov             - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @author bastian
 */
public class TransplantOptionsPage extends HgWizardPage {

	private final HgRoot hgRoot;
	private boolean merge;
	private String mergeNodeId;
	private boolean prune;
	private String pruneNodeId;
	private String filter;
	private boolean filterChangesets;
	private Button filterChangesetsCheckBox;
	private Text filterTextField;
	private ChangesetTable mergeNodeIdTable;
	private Button mergeCheckBox;
	private Button pruneCheckBox;
	private ChangesetTable pruneNodeIdTable;
	private final SortedSet<ChangeSet> changesets;

	public TransplantOptionsPage(String pageName, String title, ImageDescriptor titleImage,
			HgRoot hgRoot) {
		super(pageName, title, titleImage);
		this.hgRoot = hgRoot;
		changesets = new TreeSet<ChangeSet>(Collections.reverseOrder());
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);
		addOtherOptionsGroup(composite);
		setControl(composite);
		setPageComplete(true);
		validatePage();
	}

	@Override
	public void setPageComplete(boolean complete) {
		if(complete){
			try {
				if(HgStatusClient.isDirty(hgRoot)){
					setErrorMessage("Outstanding uncommitted changes! Transplant is not possible.");
					super.setPageComplete(false);
					return;
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		super.setPageComplete(complete);
	}

	private void addOtherOptionsGroup(Composite composite) {
		createMergeGroup(composite);
		createPruneGroup(composite);
		createFilterGroup(composite);
	}

	private void createFilterGroup(Composite composite) {
		Group filterGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("TransplantOptionsPage.filtergroup.title")); //$NON-NLS-1$

		filterChangesetsCheckBox = SWTWidgetHelper.createCheckBox(filterGroup,
				Messages.getString("TransplantOptionsPage.filterCheckBox.title")); //$NON-NLS-1$

		SelectionListener filterChangesetsCheckBoxListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				filterChangesets = filterChangesetsCheckBox.getSelection();
				filterTextField.setEnabled(filterChangesets);
				validatePage();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		filterChangesetsCheckBox.addSelectionListener(filterChangesetsCheckBoxListener);

		SWTWidgetHelper.createLabel(filterGroup, Messages.getString("TransplantOptionsPage.filterLabel.title")); //$NON-NLS-1$
		filterTextField = SWTWidgetHelper.createTextField(filterGroup);
		filterTextField.setEnabled(false);

		ModifyListener filterListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				filter = filterTextField.getText().trim();
				validatePage();
			}
		};

		filterTextField.addModifyListener(filterListener);
	}

	private void createPruneGroup(Composite composite) {
		// prune
		Group pruneGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("TransplantOptionsPage.pruneGroup.title")); //$NON-NLS-1$
		pruneCheckBox = SWTWidgetHelper.createCheckBox(pruneGroup, Messages
				.getString("TransplantOptionsPage.pruneCheckBox.title")); //$NON-NLS-1$

		SelectionListener pruneCheckBoxListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				prune = pruneCheckBox.getSelection();
				pruneNodeIdTable.setEnabled(prune);
				populatePruneNodeIdTable();
				validatePage();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		pruneCheckBox.addSelectionListener(pruneCheckBoxListener);

		pruneNodeIdTable = new ChangesetTable(pruneGroup, hgRoot);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 200;
		gridData.minimumHeight = 50;
		pruneNodeIdTable.setLayoutData(gridData);
		pruneNodeIdTable.setEnabled(false);
		SelectionListener pruneTableListener = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				ChangeSet changeSet = pruneNodeIdTable.getSelection();
				pruneNodeId = changeSet == null? null : changeSet.getChangeset();
				validatePage();
			}
		};
		pruneNodeIdTable.addSelectionListener(pruneTableListener);
	}

	private void createMergeGroup(Composite composite) {
		// other options
		Group mergeGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("TransplantOptionsPage.mergeGroup.title")); //$NON-NLS-1$

		// merge at revision
		mergeCheckBox = SWTWidgetHelper.createCheckBox(mergeGroup, Messages
				.getString("TransplantOptionsPage.mergeCheckBox.title")); //$NON-NLS-1$

		SelectionListener mergeCheckBoxListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				merge = mergeCheckBox.getSelection();
				mergeNodeIdTable.setEnabled(merge);
				populateMergeNodeIdTable();
				validatePage();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		mergeCheckBox.addSelectionListener(mergeCheckBoxListener);
		mergeNodeIdTable = new ChangesetTable(mergeGroup, hgRoot);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 200;
		gridData.minimumHeight = 50;
		mergeNodeIdTable.setLayoutData(gridData);
		mergeNodeIdTable.setEnabled(false);

		SelectionListener mergeTableListener = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			public void widgetSelected(SelectionEvent e) {
				ChangeSet changeSet = mergeNodeIdTable.getSelection();
				mergeNodeId = changeSet == null? null : changeSet.getChangeset();
				validatePage();
			}
		};
		mergeNodeIdTable.addSelectionListener(mergeTableListener);
		populateMergeNodeIdTable();
	}

	private void loadChangesets() {
		if (changesets.isEmpty()) {
			TransplantPage page = (TransplantPage) getPreviousPage();
			changesets.addAll(page.getChangesets());
		}
	}

	private void validatePage() {
		boolean valid = true;
		try {
			if (merge) {
				valid &= !StringUtils.isEmpty(mergeNodeId);
				if(!valid){
					setErrorMessage("Please select merge changeset!");
					return;
				}
			}
			if (prune) {
				valid &= !StringUtils.isEmpty(pruneNodeId);
				if(!valid){
					setErrorMessage("Please select prune changeset!");
					return;
				}
			}

			if (filterChangesets) {
				valid &= !StringUtils.isEmpty(filter);
				if(!valid){
					setErrorMessage("Please enter changeset filter!");
					return;
				}
			}
		} finally {
			if(valid){
				setErrorMessage(null);
			}
			if(isPageComplete() ^ valid) {
				setPageComplete(valid);
			}
		}
	}

	private void populatePruneNodeIdTable() {
		loadChangesets();
		pruneNodeIdTable.setChangesets(changesets.toArray(new ChangeSet[changesets.size()]));
	}

	private void populateMergeNodeIdTable() {
		loadChangesets();
		mergeNodeIdTable.setChangesets(changesets.toArray(new ChangeSet[changesets.size()]));
	}

	public boolean isMerge() {
		return merge;
	}

	public String getMergeNodeId() {
		return mergeNodeId;
	}

	public boolean isPrune() {
		return prune;
	}

	public String getPruneNodeId() {
		return pruneNodeId;
	}

	public boolean isFilterChangesets() {
		return filterChangesets;
	}

	public String getFilter() {
		return filter;
	}
}
