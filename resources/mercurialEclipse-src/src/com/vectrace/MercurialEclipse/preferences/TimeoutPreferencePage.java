/*******************************************************************************
 * Copyright (c) 2006-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Ahlberg            - implementation
 *     VecTrace (Zingo Andersen) - updateing it
 *     Jérôme Nègre              - adding label decorator section
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov - bug fixes
 *     Philip Graf               - added default timeout text field
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By sub classing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class TimeoutPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	// Default is 6 minutes. Don't ask why 6... because it is 7 times smaller than 42?
	public static final int DEFAULT_TIMEOUT = 6 * 60 * 1000;

	private static final class TimeoutFieldEditor extends IntegerFieldEditor {
		private TimeoutFieldEditor(String name, String labelText,
				Composite parent) {
			super(name, labelText, parent);
		}

		@Override
		public void load() {
			super.load();
			if (getIntValue() <= 0) {
				super.setPresentsDefaultValue(true);
				super.setStringValue(String.valueOf(DEFAULT_TIMEOUT));
			}
		}
	}

	public TimeoutPreferencePage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault()
				.getPreferenceStore());
		setDescription(Messages.getString("TimeoutPreferencePage.description")); //$NON-NLS-1$
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {

		// timeout preferences
		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.DEFAULT_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.default"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.CLONE_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.clone"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.PUSH_TIMEOUT, Messages.getString("TimeoutPreferencePage.field.push"), //$NON-NLS-1$
				getFieldEditorParent()));

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.PULL_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.pull"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.UPDATE_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.update"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.COMMIT_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.commit"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.IMERGE_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.imerge"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.LOG_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.log"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.STATUS_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.status"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.ADD_TIMEOUT, Messages.getString("TimeoutPreferencePage.field.add"), //$NON-NLS-1$
				getFieldEditorParent()));

		addField(new TimeoutFieldEditor(
				MercurialPreferenceConstants.REMOVE_TIMEOUT,
				Messages.getString("TimeoutPreferencePage.field.remove"), getFieldEditorParent())); //$NON-NLS-1$
	}

	public void init(IWorkbench workbench) {
	}

}