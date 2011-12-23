/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public class IgnoreDialog extends Dialog {

	public enum ResultType {
		FILE, FOLDER, EXTENSION, GLOB, REGEXP
	}

	private ResultType resultType;
	private IFile file;
	private IFolder folder;

	Text patternText;
	private String pattern;

	public IgnoreDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	public IgnoreDialog(Shell parentShell, IFile file) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.file = file;
	}

	public IgnoreDialog(Shell parentShell, IFolder folder) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.folder = folder;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.getString("IgnoreDialog.shell.text")); //$NON-NLS-1$
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

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout(1, true);
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.getString("IgnoreDialog.selectLabel.text")); //$NON-NLS-1$

		if(this.file != null) {
			addButton(composite, Messages.getString("IgnoreDialog.onlyFileBtn.label")+this.file.getName()+"')", false, ResultType.FILE); //$NON-NLS-1$ //$NON-NLS-2$
			if(this.file.getFileExtension() != null) {
				addButton(composite, Messages.getString("IgnoreDialog.filesWithExtBtn.label")+this.file.getFileExtension()+"')", false, ResultType.EXTENSION); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if(this.folder != null) {
			addButton(composite, Messages.getString("IgnoreDialog.folderBtn.label")+this.folder.getName()+"')", false, ResultType.FOLDER); //$NON-NLS-1$ //$NON-NLS-2$
		}
		addButton(composite, Messages.getString("IgnoreDialog.customRegExpBtn.label"), true, ResultType.REGEXP); //$NON-NLS-1$
		addButton(composite, Messages.getString("IgnoreDialog.customGlobBtn.label"), true, ResultType.GLOB); //$NON-NLS-1$

		patternText = new Text(composite, SWT.BORDER | SWT.DROP_DOWN);
		patternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return composite;
	}

	private void addButton(Composite parent, String text, final boolean isPattern, final ResultType type) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(text);
		button.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				patternText.setEnabled(isPattern);
				resultType = type;
			}
		});
	}

	@Override
	protected void okPressed() {
		this.pattern = patternText.getText();
		super.okPressed();
	}

	public IFile getFile() {
		return file;
	}

	public IFolder getFolder() {
		return folder;
	}

	public String getPattern() {
		return pattern;
	}

	public ResultType getResultType() {
		return resultType;
	}

}
