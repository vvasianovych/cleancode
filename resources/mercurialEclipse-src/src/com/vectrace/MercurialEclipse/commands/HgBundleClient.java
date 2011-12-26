/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Bastian Doetsch - implementation
 ******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgBundleClient extends AbstractClient {
	public static String bundle(HgRoot root, ChangeSet rev, String targetRepo,
			String file, boolean all, String base) throws HgException {

		AbstractShellCommand cmd = new HgCommand("bundle", "Exporting revisions to bundle", root, true);
		cmd.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(
				root));
		cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);


		if (rev != null) {
			cmd.addOptions("-r", rev.getChangeset());
		}

		if (all) {
			cmd.addOptions("-a");
		} else if (base != null && base.length() > 0) {
			cmd.addOptions("--base", base);
		}
		cmd.addOptions(file);

		if (targetRepo != null && targetRepo.length() > 0) {
			cmd.addOptions(targetRepo);
		}
		return cmd.executeToString();
	}

	public static String unbundle(HgRoot root, boolean update, String file)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("unbundle", "Importing revisions from bundle", root, true);
		cmd.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(
				root));
		cmd.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);

		if (update) {
			cmd.addOptions("-u");
		}

		cmd.addOptions(file);
		return cmd.executeToString();
	}
}
