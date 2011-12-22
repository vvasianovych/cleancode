/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.io.IOException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Properties page for hg repository, which allows to change some basic properties like login
 * name/password etc
 */
public class RepoPropertiesPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private static final String KEY_LNAME = "lname";
	private static final String KEY_LOCATION = "location";
	private static final String KEY_LOGIN_NAME = "name";
	private static final String KEY_LOGIN_PWD = "pwd";

	private IAdaptable adaptable;

	public RepoPropertiesPage() {
		super(GRID);
		PreferenceStore store = new PreferenceStore() {
			@Override
			public void save() throws IOException {
				internalSave();
			}
		};
		setPreferenceStore(store);
	}

	protected void internalSave() {
		if (adaptable == null) {
			return;
		}
		IHgRepositoryLocation adapter = (IHgRepositoryLocation) adaptable
				.getAdapter(IHgRepositoryLocation.class);
		if (!(adapter instanceof HgRepositoryLocation)) {
			return;
		}
		HgRepositoryLocation repo = (HgRepositoryLocation) adapter;
		IPreferenceStore store = getPreferenceStore();
		String user = store.getString(KEY_LOGIN_NAME);
		String pwd = store.getString(KEY_LOGIN_PWD);
		String lname = store.getString(KEY_LNAME);
		try {
			adaptable = MercurialEclipsePlugin.getRepoManager().updateRepoLocation(null,
					repo.getLocation(), lname, user, pwd);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		// as selection is not updated in the view, simply copy new data to the old one
		repo.setUser(user);
		repo.setPassword(pwd);
		repo.setLogicalName(lname);
	}

	@Override
	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	@Override
	protected void createFieldEditors() {
		IHgRepositoryLocation location = getLocation();
		boolean editable = location != null && !location.isLocal();

		StringFieldEditor locationEditor = new StringFieldEditor(KEY_LOCATION, "Location",
				getFieldEditorParent());
		addField(locationEditor);
		locationEditor.getTextControl(getFieldEditorParent()).setEditable(false);

		StringFieldEditor logicalNameEditor = new StringFieldEditor(KEY_LNAME, "Logical Name",
				getFieldEditorParent());
		addField(logicalNameEditor);
		logicalNameEditor.getTextControl(getFieldEditorParent()).setEditable(editable);

		StringFieldEditor nameEditor = new StringFieldEditor(KEY_LOGIN_NAME, "Login",
				getFieldEditorParent());
		addField(nameEditor);
		nameEditor.getTextControl(getFieldEditorParent()).setEditable(editable);

		StringFieldEditor pwdEditor = new StringFieldEditor(KEY_LOGIN_PWD, "Password",
				getFieldEditorParent());
		pwdEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		pwdEditor.getTextControl(getFieldEditorParent()).setEditable(editable);
		addField(pwdEditor);
	}

	public IAdaptable getElement() {
		return adaptable;
	}

	private IHgRepositoryLocation getLocation() {
		if (adaptable == null) {
			return null;
		}
		return (IHgRepositoryLocation) adaptable.getAdapter(IHgRepositoryLocation.class);
	}

	public void setElement(IAdaptable element) {
		this.adaptable = element;
		IHgRepositoryLocation repo = getLocation();
		if (repo == null) {
			return;
		}
		IPreferenceStore store = getPreferenceStore();
		if (repo.getUser() != null) {
			store.setDefault(KEY_LOGIN_NAME, repo.getUser());
		}
		if (repo.getPassword() != null) {
			store.setDefault(KEY_LOGIN_PWD, repo.getPassword());
		}
		if (repo.getLocation() != null) {
			store.setDefault(KEY_LOCATION, repo.getLocation());
		}
		if (repo.getLogicalName() != null) {
			store.setDefault(KEY_LNAME, repo.getLogicalName());
		}
	}

}
