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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author Andrei
 */
public class MergeDialog extends CommitDialog {

	public MergeDialog(Shell shell, HgRoot hgRoot, String defaultCommitMessage) {
		super(shell, hgRoot, null);

		options.defaultCommitMessage = defaultCommitMessage;
		// not available when merging
		options.showAmend = false;
		// don't create it as we don't want it in merge dialog
		options.showCloseBranch = false;
		options.showRevert = false;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		getShell().setText(Messages.getString("MergeDialog.window.title")); //$NON-NLS-1$
		setTitle(Messages.getString("MergeDialog.title")); //$NON-NLS-1$";
		setMessage(Messages.getString("MergeDialog.message")); //$NON-NLS-1$";
		return control;
	}

	@Override
	protected CommitFilesChooser createFilesList(Composite container) {
		SWTWidgetHelper.createLabel(container, Messages.getString("CommitDialog.selectFiles")); //$NON-NLS-1$
		return new CommitFilesChooser(root, container, false, true, true, false);
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch) throws CoreException {
		return CommitMergeHandler.commitMerge(root, getUser(), messageToCommit);
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch, ChangeSet cs) throws CoreException {
		return performCommit(messageToCommit, closeBranch);
	}
}
