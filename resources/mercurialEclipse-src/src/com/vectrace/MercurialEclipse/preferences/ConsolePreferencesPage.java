/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

public class ConsolePreferencesPage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public ConsolePreferencesPage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault()
				.getPreferenceStore());
	}

	private ColorFieldEditor commandColorEditor;
	private ColorFieldEditor messageColorEditor;
	private ColorFieldEditor errorColorEditor;
	private BooleanFieldEditor showOnMessage;
	private BooleanFieldEditor restrictOutput;
	private BooleanFieldEditor wrap;
	private IntegerFieldEditor highWaterMark;
	private IntegerFieldEditor width;
	private BooleanFieldEditor debug;
	private BooleanFieldEditor debugTime;
	private BooleanFieldEditor showOnStartup;

	@Override
	protected void createFieldEditors() {
		final Composite composite = getFieldEditorParent();
		createLabel(composite, Messages.getString("ConsolePreferencesPage.header")); //$NON-NLS-1$
		IPreferenceStore store = getPreferenceStore();

		// ** WRAP
		wrap = new BooleanFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_WRAP, Messages.getString("ConsolePreferencesPage.wrapText"), //$NON-NLS-1$
				composite);
		addField(wrap);

		width = new IntegerFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_WIDTH,
				Messages.getString("ConsolePreferencesPage.consoleWidth"), composite); //$NON-NLS-1$
		addField(width);

		// ** RESTRICT OUTPUT
		restrictOutput = new BooleanFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_LIMIT_OUTPUT,
				Messages.getString("ConsolePreferencesPage.limitOutput"), composite); //$NON-NLS-1$
		addField(restrictOutput);

		highWaterMark = new IntegerFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_HIGH_WATER_MARK,
				Messages.getString("ConsolePreferencesPage.numberChars"), composite); // ) //$NON-NLS-1$

		addField(highWaterMark);

		// ** SHOW AUTOMATICALLY
		showOnMessage = new BooleanFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE,
				Messages.getString("ConsolePreferencesPage.showConsoleOnMsg"), composite); //$NON-NLS-1$
		addField(showOnMessage);

		// ** show on startup
		showOnStartup = new BooleanFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_STARTUP,
				"Show console on start-up", composite); //$NON-NLS-1$
		addField(showOnStartup);

		// ** SHOW DEBUG
		debug = new BooleanFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_DEBUG,
				Messages.getString("ConsolePreferencesPage.showAllHgMsg"), composite); //$NON-NLS-1$
		addField(debug);

		// ** SHOW TIME
		debugTime = new BooleanFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_DEBUG_TIME,
				Messages.getString("ConsolePreferencesPage.showCommandExecutionTime"), composite); //$NON-NLS-1$
		addField(debugTime);

		createLabel(composite, Messages.getString("ConsolePreferencesPage.colorPrefs")); //$NON-NLS-1$

		// ** COLORS AND FONTS
		commandColorEditor = SWTWidgetHelper.createColorFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_COMMAND_COLOR,
				Messages.getString("ConsolePreferencesPage.cmdColor"), composite, this, getPreferenceStore()); //$NON-NLS-1$
		addField(commandColorEditor);

		messageColorEditor = SWTWidgetHelper.createColorFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_MESSAGE_COLOR,
				Messages.getString("ConsolePreferencesPage.msgColor"), composite, this, getPreferenceStore()); //$NON-NLS-1$
		addField(messageColorEditor);

		errorColorEditor = SWTWidgetHelper.createColorFieldEditor(
				MercurialPreferenceConstants.PREF_CONSOLE_ERROR_COLOR,
				Messages.getString("ConsolePreferencesPage.errorColor"), composite, this, getPreferenceStore()); //$NON-NLS-1$
		addField(errorColorEditor);

		//initIntegerFields();
		width.setEnabled(store
				.getBoolean(MercurialPreferenceConstants.PREF_CONSOLE_WRAP),
				composite);
		highWaterMark
				.setEnabled(
						store
								.getBoolean(MercurialPreferenceConstants.PREF_CONSOLE_LIMIT_OUTPUT),
						composite);
		Dialog.applyDialogFont(composite);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		initIntegerFields();
		highWaterMark.setEnabled(restrictOutput.getBooleanValue(),
				getFieldEditorParent());
		width.setEnabled(wrap.getBooleanValue(), getFieldEditorParent());
	}

	private void initIntegerFields() {
		int currWatermark = highWaterMark.getIntValue();
		if (currWatermark < 1000) {
			highWaterMark.setValidRange(1000, Integer.MAX_VALUE - 1);
			highWaterMark.setStringValue("100000");             //$NON-NLS-1$
		}
		int currWidth = width.getIntValue();
		if (currWidth < 80) {
			width.setStringValue("80"); //$NON-NLS-1$
			width.setValidRange(80, Integer.MAX_VALUE - 1);
		}
	}

	/**
	 * Utility method that creates a label instance and sets the default layout
	 * data.
	 *
	 * @param parent
	 *            the parent for the new label
	 * @param text
	 *            the text for the new label
	 * @return the new label
	 */
	private Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	public void init(IWorkbench workbench) {
	}


	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		MercurialEclipsePlugin.getDefault().savePluginPreferences();
		return ok;
	}
}
