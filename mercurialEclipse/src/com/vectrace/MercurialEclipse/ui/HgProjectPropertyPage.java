/*******************************************************************************
 * Copyright (c) 2005-2009 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov  - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.*;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.wizards.NewLocationWizard;

/**
 * @author bastian
 */
public class HgProjectPropertyPage extends PropertyPage {

	public static final String ID = "com.vectrace.MercurialEclipse.ui.HgProjectPropertyPage";
	private IProject project;
	private Group reposGroup;
	private Text defTextField;
	private HgRoot hgRoot;
	private Combo allReposCombo;
	private Text commitText;

	public HgProjectPropertyPage() {
		super();
	}

	@Override
	protected Control createContents(Composite parent) {
		this.project = (IProject) super.getElement();

		Composite comp = createComposite(parent, 1);

		if (!MercurialTeamProvider.isHgTeamProviderFor(project)) {
			setMessage("This project doesn't use MercurialEclipse as Team provider.");
			return comp;
		}
		hgRoot = MercurialTeamProvider.getHgRoot(project);
		if(hgRoot == null) {
			setMessage("Failed to find hg root for project.");
			return comp;
		}

		final HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		Set<IHgRepositoryLocation> allRepos = mgr.getAllRepoLocations();
		IHgRepositoryLocation defLoc = mgr.getDefaultRepoLocation(hgRoot);

		createCommitGroup(comp);

		createRepoGroup(comp);

		createButtons(mgr);

		updateSelection(allRepos, defLoc);

		allReposCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateDefaultRepo();
			}
		});

		updateApplyButton();
		commitText.setFocus();
		return comp;
	}

	private void createRepoGroup(Composite comp) {
		reposGroup = SWTWidgetHelper.createGroup(comp, "Default repository", 1,
				GridData.FILL_HORIZONTAL);

		defTextField = createTextField(reposGroup);
		defTextField.setEditable(false);

		createLabel(reposGroup, "Set as default:");
		allReposCombo = createCombo(reposGroup);
	}

	private void createCommitGroup(Composite comp) {
		Group commitGroup = SWTWidgetHelper.createGroup(comp, "Hg root settings", 1,
				GridData.FILL_HORIZONTAL);
		Composite commitComp = createComposite(commitGroup, 2);
		createLabel(commitComp, "Default commit name:");
		commitText = createTextField(commitComp);
		commitText.setText(HgCommitMessageManager.getDefaultCommitName(hgRoot));
	}

	private void createButtons(final HgRepositoryLocationManager mgr) {
		Composite buttonComposite = createComposite(reposGroup, 3);
		final Button addRepo = createPushButton(buttonComposite, "Add...", 1);
		addRepo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				NewLocationWizard wizard = new NewLocationWizard();
				WizardDialog dialog = new WizardDialog(addRepo.getShell(), wizard);
				int result = dialog.open();
				if(result != Window.OK){
					return;
				}
				if(wizard.getRepository() != null){
					updateSelection(mgr.getAllRepoLocations(), wizard.getRepository());
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		final Button modifyRepo = createPushButton(buttonComposite, "Modify...", 1);
		modifyRepo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (defTextField.getData() != null) {
					PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(addRepo
							.getShell(), (IAdaptable) defTextField.getData(),
							"com.vectrace.MercurialEclipse.repositoryProperties", null, null);
					dialog.open();
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		final Button deleteRepo = createPushButton(buttonComposite, "Delete...", 1);
		deleteRepo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				IHgRepositoryLocation repo = (IHgRepositoryLocation) defTextField.getData();
				if (repo != null) {
					Set<HgRoot> locationRoots = mgr.getAllRepoLocationRoots(repo);
					String message;
					if ((locationRoots.size() == 1 && !locationRoots.contains(hgRoot))
							|| locationRoots.size() > 1) {
						message = "Delete repository '" + repo.getLocation() + "'?"
								+ "\nAt least one other project still uses this repository!";
					} else {
						message = "Delete repository '" + repo.getLocation() + "'?";
					}
					boolean confirm = MessageDialog.openConfirm(getShell(), "Mercurial", message);
					if(confirm){
						mgr.disposeRepository(repo);
						updateSelection(mgr.getAllRepoLocations(), null);
					}
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private void updateSelection(final Set<IHgRepositoryLocation> allRepos,
			IHgRepositoryLocation defLoc) {
		allReposCombo.removeAll();

		int idx = -1;
		int defIndex = idx;
		for (final IHgRepositoryLocation repo : allRepos) {
			idx++;
			allReposCombo.add(repo.getLocation());
			if(defLoc != null && defLoc.equals(repo)){
				defIndex = idx;
			}
		}
		if(defIndex >= 0) {
			allReposCombo.select(defIndex);
		} else if(idx >= 0){
			allReposCombo.select(idx);
		}
		setSelectedRepo(defLoc);
	}

	@Override
	public boolean performOk() {
		if (hgRoot == null) {
			return super.performOk();
		}
		final HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		IHgRepositoryLocation defLoc = mgr.getDefaultRepoLocation(hgRoot);
		IHgRepositoryLocation data = (IHgRepositoryLocation) defTextField.getData();
		if (data != null
				&& (defLoc == null || !defTextField.getText().equals(defLoc.getLocation()))) {
			mgr.setDefaultRepository(hgRoot, data);
		}
		String commitName = commitText.getText().trim();
		String defaultCommitName = HgCommitMessageManager.getDefaultCommitName(hgRoot);
		if(!defaultCommitName.equals(commitName)) {
			HgCommitMessageManager.setDefaultCommitName(hgRoot, commitName);
		}
		return super.performOk();
	}


	@Override
	protected void performApply() {
		this.performOk();
	}

	@Override
	public boolean isValid() {
		if (hgRoot == null) {
			return super.isValid();
		}
		IHgRepositoryLocation data = (IHgRepositoryLocation) defTextField.getData();
		if (data == null) {
			return false;
		}
		String commitName = commitText.getText().trim();
		if(commitName.length() == 0) {
			return false;
		}
		return super.isValid();
	}

	@Override
	protected void performDefaults() {
		if (hgRoot == null) {
			return;
		}
		final HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		IHgRepositoryLocation defLoc = mgr.getDefaultRepoLocation(hgRoot);
		if(defLoc != null){
			defTextField.setText(defLoc.getLocation());
			defTextField.setData(defLoc);
		} else {
			defTextField.setText("");
			defTextField.setData(null);
		}
		Set<IHgRepositoryLocation> allRepos = mgr.getAllRepoLocations();
		updateSelection(allRepos, defLoc);
		commitText.setText(HgCommitMessageManager.getDefaultCommitName(hgRoot));
		super.performDefaults();
	}

	private void updateDefaultRepo() {
		HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		final Set<IHgRepositoryLocation> repos = mgr.getAllRepoLocations();
		for (IHgRepositoryLocation repo : repos) {
			if(repo.getLocation().equals(allReposCombo.getText())){
				setSelectedRepo(repo);
			}
		}
	}

	private void setSelectedRepo(IHgRepositoryLocation repo) {
		defTextField.setData(repo);
		if(repo != null) {
			defTextField.setText(repo.getLocation());
		} else {
			defTextField.setText("");
		}
		updateApplyButton();
	}
}
