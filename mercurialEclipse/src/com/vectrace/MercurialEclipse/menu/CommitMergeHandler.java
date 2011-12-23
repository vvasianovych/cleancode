/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.dialogs.MergeDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class CommitMergeHandler extends RunnableHandler {

	/**
	 * run the commit merge handler
	 */
	@Override
	public void run(HgRoot hgRoot) throws HgException {
		commitMergeWithCommitDialog(hgRoot, getShell());
	}

	/**
	 * Opens the Commit dialog and commits the merge if ok is pressed.
	 * @param hgRoot
	 *            the root to commit
	 * @return the hg command output
	 * @throws HgException
	 */
	public String commitMergeWithCommitDialog(HgRoot hgRoot, Shell shell) throws HgException {
		Assert.isNotNull(hgRoot);
		String changesetMessage = Messages.getString("CommitMergeHandler.mergeWith");
		String mergeChangesetId = MercurialStatusCache.getInstance().getMergeChangesetId(hgRoot);
		if(mergeChangesetId != null) {
			changesetMessage += " " + mergeChangesetId;
		} else {
			// TODO get the changeset id from mercurial via command call
			changesetMessage = "Merging...";
		}

		CommitDialog commitDialog = new MergeDialog(shell,  hgRoot,	changesetMessage);

		// open dialog and wait for ok
		commitDialog.open();
		return commitDialog.getCommitResult();
	}

	/**
	 * Commits a merge with the given message. The commit dialog is not shown.
	 * @param hgRoot the root to be committed, not null
	 * @return the output of hg commit
	 * @throws HgException
	 * @throws CoreException
	 */
	public static String commitMerge(HgRoot hgRoot, String commitName, String message)
			throws HgException, CoreException {
		Assert.isNotNull(hgRoot);
		Assert.isNotNull(message);

		// do hg call
		String result = HgCommitClient.commit(hgRoot, commitName, message);
		return result;
	}

}
