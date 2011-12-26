/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.RebaseOperation;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Dialog for showing which files will be committed for a rebase.
 * <p>
 * Future: For --collapse rebases allow the user to set commit message and commit user when the
 * rebase is started. Future: The user shouldn't be displayed.
 */
public class ContinueRebaseDialog extends CommitDialog {

	public ContinueRebaseDialog(Shell shell, HgRoot hgRoot) {
		super(shell, hgRoot, null);

		options.defaultCommitMessage = "";
		options.showAmend = false;
		options.showCloseBranch = false;
		options.showRevert = false;
		options.showCommitMessage = false;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		getShell().setText(Messages.getString("RebaseDialog.window.title")); //$NON-NLS-1$
		setTitle(Messages.getString("RebaseDialog.title")); // ";
		setMessage(Messages.getString("RebaseDialog.message"));
		return control;
	}

	@Override
	protected CommitFilesChooser createFilesList(Composite container) {
		SWTWidgetHelper.createLabel(container, Messages.getString("RebaseDialog.fileList"));
		return new CommitFilesChooser(root, container, false, false, false, false);
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch)
			throws CoreException {
		return continueRebase(messageToCommit);
	}

	@Override
	protected String performCommit(String messageToCommit, boolean closeBranch, ChangeSet cs)
			throws CoreException {
		return continueRebase(messageToCommit);
	}

	private String continueRebase(String messageToCommit) throws CoreException {
		RebaseOperation op = RebaseOperation.createContinue(MercurialEclipsePlugin.getActiveWindow(), root, getUser());

		try {
			op.run(new NullProgressMonitor());
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.rethrow(e);
		} catch (InterruptedException e) {
			MercurialEclipsePlugin.rethrow(e);
		}

		return op.getResult();
	}
}
