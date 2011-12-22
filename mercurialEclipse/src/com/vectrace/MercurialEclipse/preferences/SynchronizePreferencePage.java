/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov		 - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class SynchronizePreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public SynchronizePreferencePage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault().getPreferenceStore());
		setDescription(Messages.getString("SynchronizePreferencePage.description")); //$NON-NLS-1$
	}

	public void init(IWorkbench workbench) {
		// noop
	}

	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(
				PREF_SYNC_ONLY_CURRENT_BRANCH,
				Messages.getString("SynchronizePreferencePage.syncOnlyCurrentBranch"), //$NON-NLS-1$
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				PREF_SYNC_ALL_PROJECTS_IN_REPO,
				Messages.getString("SynchronizePreferencePage.syncAllProjectsInRepo"), //$NON-NLS-1$
				getFieldEditorParent()));

	}

}