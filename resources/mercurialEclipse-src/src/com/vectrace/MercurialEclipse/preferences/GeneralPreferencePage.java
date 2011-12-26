/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
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
 *     Andrei Loskutov           - bug fixes
 *     Ilya Ivanov (Intland)	 - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.HgFeatures;
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

public class GeneralPreferencePage extends FieldEditorPreferencePage implements
IWorkbenchPreferencePage {

	private MercurialExecutableFileFieldEditor execField;

	private static final class LabelDecoratorRadioGroupFieldEditor extends
	RadioGroupFieldEditor {
		private LabelDecoratorRadioGroupFieldEditor(String name,
				String labelText, int numColumns, String[][] labelAndValues,
				Composite parent, boolean useGroup) {
			super(name, labelText, numColumns, labelAndValues, parent, useGroup);
		}

		@Override
		protected void doStore() {
			super.doStore();
			MercurialEclipsePlugin.getDefault().checkHgInstallation();
			// ResourceDecorator.onConfigurationChanged();
		}
	}

	private static final class MercurialExecutableFileFieldEditor extends FileFieldEditor {
		private MercurialExecutableFileFieldEditor(String name,
				String labelText, Composite parent) {
			super(name, labelText, false, StringFieldEditor.VALIDATE_ON_KEY_STROKE, parent);
		}

		@Override
		public boolean checkState() {
			// There are other ways of doing this properly but this is
			// better than the default behavior
			String stringValue = getStringValue();
			if(MERCURIAL_EXECUTABLE.equals(stringValue)){
				clearErrorMessage();
				return true;
			}
			return super.checkState();
		}
	}

	private static final class GpgExecutableFileFieldEditor extends FileFieldEditor {
		private GpgExecutableFileFieldEditor(String name, String labelText,
				Composite parent) {
			super(name, labelText, false, StringFieldEditor.VALIDATE_ON_KEY_STROKE, parent);
		}

		@Override
		protected boolean checkState() {
			// There are other ways of doing this properly but this is
			// better than the default behaviour
			if(GPG_EXECUTABLE.equals(getStringValue())){
				clearErrorMessage();
				return true;
			}
			return super.checkState();
		}
	}

	public GeneralPreferencePage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault().getPreferenceStore());
		setDescription(Messages.getString("GeneralPreferencePage.description")); //$NON-NLS-1$
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {

		File integratedHgExecutable = PreferenceInitializer.getIntegratedHgExecutable();
		if(integratedHgExecutable != null){
			addField(new BooleanFieldEditor(USE_BUILT_IN_HG_EXECUTABLE,
					"Use default (built-in) Mercurial executable",
					getFieldEditorParent()){
				@Override
				protected void fireValueChanged(String property, Object oldValue,
						Object newValue) {
					super.fireValueChanged(property, oldValue, newValue);
					if(newValue instanceof Boolean) {
						enablePathEditor(!((Boolean) newValue).booleanValue());
					}
				}
				@Override
				protected void doLoadDefault() {
					super.doLoadDefault();
					enablePathEditor(!getBooleanValue());
				}

			});
		}

		execField = new MercurialExecutableFileFieldEditor(
				MERCURIAL_EXECUTABLE,
				Messages.getString("GeneralPreferencePage.field.hgExecutable"), getFieldEditorParent());

		addField(execField);

		execField.setEmptyStringAllowed(false);

		if (!MercurialEclipsePlugin.getDefault().isHgUsable()) {
			execField.setErrorMessage(Messages.getString("GeneralPreferencePage.error.HgNotInstalled")); //$NON-NLS-1$
		}
		if (integratedHgExecutable != null && getPreferenceStore().getBoolean(USE_BUILT_IN_HG_EXECUTABLE)) {
			execField.setEnabled(false, getFieldEditorParent());
		}

		addField(new GpgExecutableFileFieldEditor(
				GPG_EXECUTABLE,
				Messages.getString("GeneralPreferencePage.field.gpgExecutable"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new StringFieldEditor(
				MERCURIAL_USERNAME,
				Messages.getString("GeneralPreferencePage.field.username"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new BooleanFieldEditor(
				PREF_USE_MERCURIAL_USERNAME,
				"Prefer 'username' value from .hgrc as default user name", getFieldEditorParent())); //$NON-NLS-1$

		addField(new BooleanFieldEditor(
				PREF_DEFAULT_REBASE_KEEP_BRANCHES,
				"Default to 'Retain the branch name' for Rebase command", getFieldEditorParent())); //$NON-NLS-1$

		addField(new BooleanFieldEditor(
				PREF_USE_EXTERNAL_MERGE,
				Messages.getString("GeneralPreferencePage.useExternalMergeTool"), getFieldEditorParent())); //$NON-NLS-1$

		BooleanFieldEditor editor = new BooleanFieldEditor(
				PREF_PUSH_NEW_BRANCH,
				Messages.getString("GeneralPreferencePage.pushNewBranches"), getFieldEditorParent());
		addField(editor);
		if(!HgFeatures.NEW_BRANCH.isEnabled()) {
			editor.setEnabled(false, getFieldEditorParent());
			editor.setLabelText(editor.getLabelText() + " " + Messages.getString("GeneralPreferencePage.optionDisabled"));
		}

		addField(new LabelDecoratorRadioGroupFieldEditor(
				LABELDECORATOR_LOGIC,
				Messages.getString("GeneralPreferencePage.field.decorationGroup.description"), //$NON-NLS-1$
				1,
				new String[][] {
					{
						Messages.getString("GeneralPreferencePage.field.decorationGroup.asModified"), //$NON-NLS-1$
						LABELDECORATOR_LOGIC_2MM },
						{
							Messages.getString("GeneralPreferencePage.field.decorationGroup.mostImportant"), //$NON-NLS-1$
							LABELDECORATOR_LOGIC_HB } },
							getFieldEditorParent(), true));

		addField(new BooleanFieldEditor(
				PREF_DECORATE_WITH_COLORS,
				Messages.getString("GeneralPreferencePage.enableFontAndColorDecorations"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new BooleanFieldEditor(
				PREF_AUTO_SHARE_PROJECTS,
				Messages.getString("GeneralPreferencePage.autoshare"), //$NON-NLS-1$
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(
				PREF_PRESELECT_UNTRACKED_IN_COMMIT_DIALOG,
				Messages.getString("GeneralPreferencePage.preselectUntrackedInCommitDialog"), //$NON-NLS-1$
				getFieldEditorParent()));

		BooleanFieldEditor cert_editor = new BooleanFieldEditor(
				PREF_VERIFY_SERVER_CERTIFICATE,
				Messages.getString("GeneralPreferencePage.verifyServerCertificate"), //$NON-NLS-1$
				getFieldEditorParent());
		addField(cert_editor);
		if(!HgFeatures.INSECURE.isEnabled()) {
			cert_editor.setEnabled(false, getFieldEditorParent());
			cert_editor.setLabelText(cert_editor.getLabelText() + " " + Messages.getString("GeneralPreferencePage.optionDisabled"));
		}

		IntegerFieldEditor commitSizeEditor = new IntegerFieldEditor(
				COMMIT_MESSAGE_BATCH_SIZE,
				Messages.getString("GeneralPreferencePage.field.commitMessageBatchSize"), //$NON-NLS-1$
				getFieldEditorParent());
		commitSizeEditor.setValidRange(1, Integer.MAX_VALUE);
		addField(commitSizeEditor);
	}

	protected void enablePathEditor(boolean on) {
		if(execField == null){
			return;
		}
		execField.setEnabled(on, getFieldEditorParent());
		if(!on){
			execField.setStringValue(PreferenceInitializer.getIntegratedHgExecutable().getPath());
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(!execField.checkState()){
			execField.showErrorMessage();
		}
	}

	@Override
	public boolean isValid() {
		return execField.checkState() && super.isValid();
	}

	public void init(IWorkbench workbench) {
	}

}