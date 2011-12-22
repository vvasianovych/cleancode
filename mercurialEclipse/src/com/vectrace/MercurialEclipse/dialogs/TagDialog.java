/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.ui.TagTable;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class TagDialog extends Dialog {

	private final HgRoot hgRoot;

	// main TabItem
	private Text nameText;
	private Button forceButton;
	private Button localButton;

	// output
	private String name;
	private String targetRevision;
	private boolean forced;
	private boolean local;

	private Text userTextField;

	private String user;

	public TagDialog(Shell parentShell, HgRoot hgRoot) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.hgRoot = hgRoot;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.getString("TagDialog.shell.text")); //$NON-NLS-1$
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
		GridLayout gridLayout = new GridLayout(2, false);
		composite.setLayout(gridLayout);

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				| GridData.FILL_VERTICAL));

		createMainTabItem(tabFolder);
		createTargetTabItem(tabFolder);
		createRemoveTabItem(tabFolder);

		return composite;
	}

	private GridData createGridData(int colspan) {
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = colspan;
		data.minimumWidth = SWT.DEFAULT;
		return data;
	}

	private GridData createGridData(int colspan, int width) {
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = colspan;
		data.minimumWidth = width;
		return data;
	}

	private void createUserCommitText(Composite container) {
		Composite comp = SWTWidgetHelper.createComposite(container, 2);
		SWTWidgetHelper.createLabel(comp, Messages.getString("CommitDialog.userLabel.text")); //$NON-NLS-1$
		userTextField = SWTWidgetHelper.createTextField(comp);
		user = HgCommitMessageManager.getDefaultCommitName(hgRoot);
		userTextField.setText(user);
	}

	protected TabItem createMainTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("TagDialog.mainTab.name")); //$NON-NLS-1$

		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));

		// tag name
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.getString("TagDialog.enterTagName")); //$NON-NLS-1$
		label.setLayoutData(createGridData(1));

		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(createGridData(1));

		createUserCommitText(composite);


		forceButton = new Button(composite, SWT.CHECK);
		forceButton.setText(Messages.getString("TagDialog.moveTag")); //$NON-NLS-1$
		forceButton.setLayoutData(createGridData(1));

		localButton = new Button(composite, SWT.CHECK);
		localButton.setText(Messages.getString("TagDialog.createLocal")); //$NON-NLS-1$
		localButton.setLayoutData(createGridData(1));

		// List of existing tags
		label = new Label(composite, SWT.NONE);
		label.setText(Messages.getString("TagDialog.existingTags")); //$NON-NLS-1$
		label.setLayoutData(createGridData(1));

		final TagTable table = new TagTable(composite, hgRoot);
		table.hideTip();
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 150;
		table.setLayoutData(data);

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Tag tag = table.getSelection();
				nameText.setText(tag.getName());
				localButton.setSelection(tag.isLocal());
			}
		});

		try {
			table.setTags(HgTagClient.getTags(hgRoot));
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}

		item.setControl(composite);
		return item;
	}

	protected TabItem createOptionsTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("TagDialog.options")); //$NON-NLS-1$

		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));

		// TODO commit date

		// user name
		final Button customUserButton = new Button(composite, SWT.CHECK);
		customUserButton.setText(Messages.getString("TagDialog.customUserName")); //$NON-NLS-1$

		final Text userText = new Text(composite, SWT.BORDER);
		userText.setLayoutData(createGridData(1, 250));
		userText.setEnabled(false);

		customUserButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				userText.setEnabled(customUserButton.getSelection());
			}
		});

		// commit message

		item.setControl(composite);
		return item;
	}

	protected TabItem createRemoveTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("TagDialog.removeTag")); //$NON-NLS-1$
		Composite composite = SWTWidgetHelper.createComposite(folder, 1);

		final TagTable tt = new TagTable(composite, hgRoot);
		tt.hideTip();

		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 150;
		tt.setLayoutData(data);

		try {
			tt.setTags(HgTagClient.getTags(hgRoot));
		} catch (HgException e2) {
			MercurialEclipsePlugin.showError(e2);
			MercurialEclipsePlugin.logError(e2);
		}
		Button removeButton = SWTWidgetHelper.createPushButton(composite,
				Messages.getString("TagDialog.removeSelectedTag"), 1); //$NON-NLS-1$
		MouseListener listener = new MouseListener() {
			public void mouseUp(MouseEvent e) {
				try {
					String result = HgTagClient.removeTag(hgRoot, tt.getSelection(), userTextField.getText());
					HgClients.getConsole().printMessage(result, null);
					tt.setTags(HgTagClient.getTags(hgRoot));
					new RefreshRootJob(com.vectrace.MercurialEclipse.menu.Messages.getString(
							"TagHandler.refreshing"), hgRoot, RefreshRootJob.ALL).schedule(); //$NON-NLS-1$
				} catch (HgException e1) {
					MercurialEclipsePlugin.showError(e1);
					MercurialEclipsePlugin.logError(e1);
				}
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		};
		removeButton.addMouseListener(listener);
		item.setControl(composite);
		return item;
	}

	protected TabItem createTargetTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("TagDialog.targetTab.name")); //$NON-NLS-1$

		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));

		Button parentButton = new Button(composite, SWT.RADIO);
		parentButton.setText(Messages.getString("TagDialog.tagParentChangeset")); //$NON-NLS-1$
		parentButton.setSelection(true);

		Button otherButton = new Button(composite, SWT.RADIO);
		otherButton.setText(Messages.getString("TagDialog.tagAnotherChangeset")); //$NON-NLS-1$

		final ChangesetTable table = new ChangesetTable(composite, hgRoot);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setEnabled(false);
		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				targetRevision = Integer.toString(table.getSelection()
						.getChangesetIndex());
			}
		});

		parentButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				table.setEnabled(false);
				targetRevision = null;
			}
		});

		otherButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
					table.setEnabled(true);
					ChangeSet changeset = table.getSelection();
					if (changeset != null) {
					targetRevision = Integer.toString(changeset
							.getChangesetIndex());
				}
			}
		});

		item.setControl(composite);
		return item;
	}

	@Override
	protected void okPressed() {
		name = nameText.getText();
		forced = forceButton.getSelection();
		local = localButton.getSelection();
		user = userTextField.getText();
		super.okPressed();
	}

	public String getName() {
		return name;
	}

	public String getTargetRevision() {
		return targetRevision;
	}

	public boolean isForced() {
		return forced;
	}

	public boolean isLocal() {
		return local;
	}

	public String getUser() {
		return user;
	}
}
