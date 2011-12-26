/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @author Andrei
 */
public class EditChangesetDialog extends CommitDialog {

	private final WorkingChangeSet changeset;
	private Text changesetNameText;

	public EditChangesetDialog(Shell shell, HgRoot hgRoot, WorkingChangeSet changeset,
			boolean isDefault) {
		super(shell, hgRoot, new ArrayList<IResource>(changeset.getFiles()));
		setBlockOnOpen(true);
		this.changeset = changeset;
		Assert.isNotNull(hgRoot);
		options.defaultCommitMessage = changeset.getComment();
		// not available
		options.showAmend = false;
		// don't create it as we don't want it in merge dialog
		options.showCloseBranch = false;
		options.showRevert = false;
		options.showDiff = true;
		options.filesSelectable = !isDefault;
		options.allowEmptyCommit = true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite control = (Composite) super.createDialogArea(parent);
		getShell().setText(Messages.getString("EditChangesetDialog.title", changeset.getName())); //$NON-NLS-1$
		setTitle(Messages.getString("EditChangesetDialog.title", changeset.getName())); //$NON-NLS-1$";
		setMessage(Messages.getString("EditChangesetDialog.message")); //$NON-NLS-1$";
		createChangesetNameContainer(control);
		return control;
	}

	@Override
	protected String getInitialCommitMessage() {
		String initialCommitMessage = super.getInitialCommitMessage();
		if(StringUtils.isEmpty(initialCommitMessage)) {
			if(!StringUtils.isEmpty(changeset.getComment())) {
				return changeset.getComment();
			}
			if(!StringUtils.isEmpty(changeset.getName())) {
				return changeset.getName();
			}
		}
		return initialCommitMessage;
	}

	private void createChangesetNameContainer(Composite container) {
		Composite comp = SWTWidgetHelper.createComposite(container, 2);
		comp.moveAbove(container.getChildren()[0]);

		SWTWidgetHelper.createLabel(comp, "Changeset name:");
		changesetNameText = SWTWidgetHelper.createTextField(comp);
		changesetNameText.setText(changeset.getName());
		changesetNameText.addFocusListener(new FocusListener() {

			private String oldCommitMessage;

			public void focusLost(FocusEvent e) {
				String text = changesetNameText.getText();
				if(text.length() > 0 && getCommitMessage().length() == 0 ||
						(oldCommitMessage != null && !text.equals(oldCommitMessage))) {
					setCommitMessage(text);
				}
			}

			public void focusGained(FocusEvent e) {
				oldCommitMessage = getCommitMessage();
				if(!changesetNameText.getText().equals(oldCommitMessage)) {
					oldCommitMessage = null;
				}
			}
		});
	}


	@Override
	protected CommitFilesChooser createFilesList(Composite container) {
		return super.createFilesList(container);
//		SWTWidgetHelper.createLabel(container, Messages.getString("CommitDialog.selectFiles")); //$NON-NLS-1$
//		return new CommitFilesChooser(root, container, options.filesSelectable, true, true, false);
	}

	@Override
	protected void okPressed() {
		changeset.setComment(getCommitMessage());
		changeset.setName(changesetNameText.getText());
		setReturnCode(OK);
		close();
		// TODO add file management
//		commitFilesList.getCheckedResources();
//		commitFilesList.getUncheckedResources(FILE_ADDED,
//				FILE_DELETED, FILE_MODIFIED, FILE_REMOVED);
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch) throws CoreException {
		return "";
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch, ChangeSet cs) throws CoreException {
		return "";
	}

}
