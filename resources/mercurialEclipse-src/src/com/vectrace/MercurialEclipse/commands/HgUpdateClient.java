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
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public class HgUpdateClient extends AbstractClient {

	public static void update(final HgRoot hgRoot, String revision, boolean clean)
			throws HgException {

		HgCommand command = new HgCommand("update", makeDescription(revision, clean), hgRoot, false); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.UPDATE_TIMEOUT);
		if (revision != null && revision.trim().length() > 0) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}
		if (clean) {
			command.addOptions("-C"); //$NON-NLS-1$
		}
		addMergeToolPreference(command);

		try {
			command.executeToBytes();
		} catch (HgException e) {
			if (e.getMessage().contains("use 'hg resolve' to retry unresolved file merges")) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					public void run() {
						MessageDialog.openInformation(null, "Unresolved conflicts",
								"You have unresolved conflicts after update. Use Synchronize View to edit conflicts");
					}
				});
			} else {
				throw e;
			}
		} finally {
			new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.LOCAL).schedule();
		}
	}

	private static String makeDescription(String revision, boolean clean) {
		revision = (revision == null || revision.trim().length() == 0) ? null : revision.trim();

		if (revision != null) {
			return ((clean) ? "Clean update" : "Updating") +  " to " + revision;
		}

		return (clean) ? "Clean update" : "Updating working directory";
	}
}
