/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 * Adam Berkes (Intland) - various fixes
 * Andrei Loskutov - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @author bastian
 *
 */
public class TransplantPage extends ConfigurationWizardMainPage {

	/** changesets sorted in the ascending revision order */
	private final SortedSet<ChangeSet> selectedChangesets;
	private boolean branch;
	private boolean all;
	private String branchName;
	private final SortedSet<ChangeSet> changesets;

	private ChangesetTable changesetTable;
	private Button branchCheckBox;
	private Combo branchNameCombo;
	private Button allCheckBox;

	public TransplantPage(String pageName, String title,
			ImageDescriptor titleImage, HgRoot hgRoot) {
		super(pageName, title, titleImage);
		setHgRoot(hgRoot);
		selectedChangesets = new TreeSet<ChangeSet>();
		changesets = new TreeSet<ChangeSet>(Collections.reverseOrder());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) getControl();

		ModifyListener urlModifyListener = new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				try {
					HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
					getIncoming(repoManager.getRepoLocation(getUrlText()));
				} catch (HgException e1) {
					clearChangesets();
					// bad URI?
					setErrorMessage(e1.getMessage());
					setPageComplete(false);
					return;
				}
			}
		};
		getUrlCombo().addModifyListener(urlModifyListener);

		addBranchGroup(composite);
		addChangesetGroup(composite);

		if (MercurialEclipsePlugin.getDefault().getPreferenceStore().getBoolean(
				MercurialPreferenceConstants.PREF_DEFAULT_TRANSPLANT_FROM_LOCAL_BRANCHES)) {
			setUseLocalBranch(true);
		}

		setPageComplete(true);
		validatePage();
	}

	@Override
	public void setPageComplete(boolean complete) {
		if(complete){
			try {
				if(HgStatusClient.isDirty(getHgRoot())){
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

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.ConfigurationWizardMainPage#finish(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean finish(IProgressMonitor monitor) {
		MercurialEclipsePlugin.getDefault().getPreferenceStore().setValue(
				MercurialPreferenceConstants.PREF_DEFAULT_TRANSPLANT_FROM_LOCAL_BRANCHES,
				branchCheckBox.getSelection());

		return super.finish(monitor);
	}

	private void validatePage() {
		boolean valid = true;
		setMessage(null, IMessageProvider.WARNING);
		try {
			if (branch) {
				valid &= !StringUtils.isEmpty(branchName);
				if(!valid){
					setErrorMessage("Please select local branch!");
					return;
				}
//				if (!all) {
//					valid &= selectedChangesets.size() == 1
//							&& Branch.same(branchName, selectedChangesets.first().getBranch());
//					if(!valid){
//						setErrorMessage("Please select exact one changeset if transplanting "
//								+ "not all changesets from the local branch!");
//						return;
//					}
//				}
				if (valid && !all && selectedChangesets.size() == 0)
				{
					setMessage("No changeset selected.", IMessageProvider.WARNING);
					return;
				}
			} else {
				valid &= !StringUtils.isEmpty(getUrlText());
				if(!valid){
					setErrorMessage("Please provide valid repository location!");
					return;
				}
				valid &= selectedChangesets.size() > 0;
				if(!valid){
					setErrorMessage("Please select at least one changeset!");
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

	private void addBranchGroup(Composite composite) {
		createBranchCheckBox(composite);
		// now the branch group
		Group branchGroup = SWTWidgetHelper.createGroup(composite, Messages
				.getString("TransplantPage.branchGroup.title")); //$NON-NLS-1$
		createAllCheckBox(branchGroup);
		createBranchNameCombo(branchGroup);
	}

	private void createBranchNameCombo(Group branchGroup) {
		SWTWidgetHelper.createLabel(branchGroup, Messages
				.getString("TransplantPage.branchLabel.title")); //$NON-NLS-1$
		branchNameCombo = SWTWidgetHelper.createCombo(branchGroup);
		branchNameCombo.setEnabled(false);
		populateBranchNameCombo();

		SelectionListener branchNameComboListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				branchName = branchNameCombo.getText();
				if (Branch.isDefault(branchName)) {
					branchName = Branch.DEFAULT;
				}
				getLocalFromBranch(branchName);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		branchNameCombo.addSelectionListener(branchNameComboListener);
	}

	private void createAllCheckBox(Group branchGroup) {
		allCheckBox = SWTWidgetHelper.createCheckBox(branchGroup, Messages
				.getString("TransplantPage.allCheckBox.title")); //$NON-NLS-1$
		allCheckBox.setEnabled(false);

		SelectionListener allCheckBoxListener = new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				all = allCheckBox.getSelection();
				changesetTable.setEnabled(!all);
				if(all){
					changesetTable.clearSelection();
					selectedChangesets.clear();
				}
				validatePage();
			}
		};

		allCheckBox.addSelectionListener(allCheckBoxListener);
	}

	private void createBranchCheckBox(Composite parent) {
		branchCheckBox = SWTWidgetHelper.createCheckBox(parent, Messages
				.getString("TransplantPage.branchCheckBox.title")); //$NON-NLS-1$

		SelectionListener branchCheckBoxListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setUseLocalBranch(branchCheckBox.getSelection());
				validatePage();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		branchCheckBox.addSelectionListener(branchCheckBoxListener);
	}

	protected void setUseLocalBranch(boolean useLocalBranch) {
		this.branch = useLocalBranch;
		if (branchCheckBox.getSelection() != useLocalBranch) {
			branchCheckBox.setSelection(useLocalBranch);
		}
		setUrlGroupEnabled(!useLocalBranch);
		getUserCombo().setEnabled(!useLocalBranch);
		passwordText.setEnabled(!useLocalBranch);
		allCheckBox.setEnabled(useLocalBranch);
		branchNameCombo.setEnabled(useLocalBranch);
		clearChangesets();
		getUrlCombo().deselectAll();
		if (useLocalBranch) {
			branchName = null;
			branchNameCombo.deselectAll();
		}
	}

	private void addChangesetGroup(Composite composite) {
		// table of changesets
		Group changeSetGroup = SWTWidgetHelper.createGroup(
				composite,
				Messages.getString("TransplantPage.changesetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 200;
		gridData.minimumHeight = 50;
		changesetTable = new ChangesetTable(changeSetGroup, SWT.MULTI
				| SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
						| SWT.H_SCROLL, getHgRoot(), false);
		changesetTable.setLayoutData(gridData);
		changesetTable.setEnabled(true);

		SelectionListener changeSetTableListener = new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				setErrorMessage(null);
				selectedChangesets.clear();
				ChangeSet[] changeSets = changesetTable.getSelections();
				if(changeSets != null && changeSets.length != 0) {
					selectedChangesets.addAll(Arrays.asList(changeSets));
				}
				validatePage();
			}
		};

		changesetTable.addSelectionListener(changeSetTableListener);
		populateChangesetTable();
	}

	private void populateBranchNameCombo() {
		try {
			Branch[] branches = HgBranchClient.getBranches(getHgRoot());
			for (Branch myBranch : branches) {
				branchNameCombo.add(myBranch.getName());
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.showError(e);
			MercurialEclipsePlugin.logError(e);
		}
	}

	private void populateChangesetTable() {
		changesetTable.clearTable();
		changesetTable.setChangesets(changesets.toArray(new ChangeSet[changesets.size()]));
	}

	public boolean isBranch() {
		return branch;
	}

	public SortedSet<ChangeSet> getSelectedChangesets() {
		return selectedChangesets;
	}

	public String getBranchName() {
		return branchName;
	}

	public boolean isAll() {
		return all;
	}

	public SortedSet<ChangeSet> getChangesets() {
		return changesets;
	}

	private void getIncoming(final IHgRepositoryLocation repoLocation) {
		FetchChangesetsOperation op = new FetchChangesetsOperation() {
			@Override
			protected Set<ChangeSet> fetchChanges(IProgressMonitor monitor) throws HgException {
				return  IncomingChangesetCache.getInstance().getChangeSets(
						getHgRoot(), repoLocation, null);
			}
		};
		run(op);
	}

	private void getLocalFromBranch(final String branchName1) {
		FetchChangesetsOperation op = new FetchChangesetsOperation() {
			@Override
			protected Set<ChangeSet> fetchChanges(IProgressMonitor monitor) throws HgException {
				return LocalChangesetCache.getInstance().getOrFetchChangeSetsByBranch(getHgRoot(),
						branchName1);
			}
		};
		run(op);
	}

	private void run(FetchChangesetsOperation runnable) {
		clearChangesets();
		try {
			getContainer().run(true, true, runnable);
			changesets.addAll(runnable.getChanges());
			populateChangesetTable();
			validatePage();
		} catch (InvocationTargetException e) {
			setErrorMessage(Messages.getString("TransplantPage.errorLoadChangesets")
					+ ": " + e.getCause().getMessage()); //$NON-NLS-1$
			setPageComplete(false);
			MercurialEclipsePlugin.logError(e.getCause());
		} catch (InterruptedException e) {
			MercurialEclipsePlugin.logError(e);
			validatePage();
		}
	}

	private void clearChangesets() {
		changesets.clear();
		changesetTable.clearTable();
		selectedChangesets.clear();
	}

	abstract static class FetchChangesetsOperation implements IRunnableWithProgress {
		Set<ChangeSet> changes = new HashSet<ChangeSet>();
		Set<ChangeSet> getChanges(){
			return changes;
		}
		public final void run(final IProgressMonitor monitor) throws InvocationTargetException {
			monitor.beginTask("Retrieving changesets...", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
			// Timer which is used to monitor the monitor cancellation
			Timer t = new Timer("Fetch data watcher", false);

			// only start timer if the operation is NOT running in the UI thread
			if(Display.getCurrent() == null){
				final Thread threadToCancel = Thread.currentThread();
				t.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						if (monitor.isCanceled() && !threadToCancel.isInterrupted()) {
							threadToCancel.interrupt();
						}
					}
				}, 500, 50);
			}
			try {
				changes = fetchChanges(monitor);
			} catch (HgException e) {
				throw new InvocationTargetException(e);
			} finally {
				t.cancel();
			}
		}
		protected abstract Set<ChangeSet> fetchChanges(IProgressMonitor monitor) throws HgException;
	}

}
