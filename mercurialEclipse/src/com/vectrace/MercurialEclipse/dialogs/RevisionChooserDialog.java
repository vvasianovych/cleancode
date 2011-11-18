/*******************************************************************************
 * Copyright (c) 2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - small changes
 *     Adam Berkes (Intland)     - bug fixes
 *     Andrei Loskutov           - bug fixes
 *     Philip Graf               - Field assistance for revision field and bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.FileDataLoader;
import com.vectrace.MercurialEclipse.storage.RootDataLoader;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class RevisionChooserDialog extends Dialog {
	private final DataLoader dataLoader;
	private final String title;
	private boolean defaultShowingHeads;
	private boolean disallowSelectingParents;
	private boolean showForceButton;
	private boolean isForceChecked;
	private String forceButtonText;
	private RevisionChooserPanel panel;

	public RevisionChooserDialog(Shell parentShell, String title, IFile file) {
		this(parentShell, title, new FileDataLoader(file));
	}

	public RevisionChooserDialog(Shell parentShell, String title, HgRoot hgRoot) {
		this(parentShell, title, new RootDataLoader(hgRoot));
	}

	private RevisionChooserDialog(Shell parentShell, String title, DataLoader loader) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.title = title;
		dataLoader = loader;
	}


	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		RevisionChooserPanel.Settings opt = new RevisionChooserPanel.Settings();
		opt.defaultShowingHeads = defaultShowingHeads;
		opt.disallowSelectingParents = disallowSelectingParents;
		opt.forceButtonText = forceButtonText;
		opt.isForceChecked = isForceChecked;
		opt.showForceButton = showForceButton;

		panel = new RevisionChooserPanel(composite, dataLoader, opt);
		panel.addSelectionListener(this);
		return composite;
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

	public void setForceChecked(boolean on){
		isForceChecked = true;
	}

	public boolean isForceChecked(){
		return isForceChecked;
	}

	public void showForceButton(boolean show){
		showForceButton = show;
	}

	public void setForceButtonText(String forceButtonText) {
		this.forceButtonText = forceButtonText;
	}

	@Override
	protected void okPressed() {
		boolean ok = panel.calculateRevision();
		if(!ok){
			return;
		}
		super.okPressed();
	}

	protected void revisionSelected() {
		super.okPressed();
	}

	public String getRevision() {
		return panel.getRevision();
	}


	public ChangeSet getChangeSet() {
		return panel.getChangeSet();
	}

	public void setDefaultShowingHeads(boolean defaultShowingHeads) {
		this.defaultShowingHeads = defaultShowingHeads;
	}

	public void setDisallowSelectingParents(boolean b) {
		this.disallowSelectingParents = b;
	}


}
