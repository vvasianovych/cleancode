/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class HgMergeClient extends AbstractClient {

	public static String merge(HgRoot hgRoot, String revision, boolean forced)
			throws HgException {
		HgCommand command = new HgCommand("merge", "Merging", hgRoot, false);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		addMergeToolPreference(command);

		if (revision != null) {
			command.addOptions("-r", revision); //$NON-NLS-1$
		}
		if (forced) {
			command.addOptions("-f"); //$NON-NLS-1$
		}

		MercurialUtilities.setMergeViewDialogShown(false);

		try {
			String result = command.executeToString();
			return result;
		} catch (HgException e) {
			// if conflicts aren't resolved and no merge tool is started, hg
			// exits with 1
			if (e.getStatus().getCode() != 1) {
				throw e;
			}
			return e.getMessage();
		}
	}
}
