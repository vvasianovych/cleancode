/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;
import java.util.regex.Pattern;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

/**
 * @author bastian
 *
 */
public class HgRebaseClient extends AbstractClient {

	private static final Pattern REBASING_CONFLICT = Pattern.compile("^abort:.*unresolved conflicts", Pattern.MULTILINE);

	/**
	 * Calls hg rebase.
	 * <p>
	 * Doesn't support supplying custom commit messages for collapse and continued collapse.
	 *
	 * @param hgRoot
	 *            a hg root that is to be rebased.
	 * @param sourceRev
	 *            --source option, -1 if not set
	 * @param baseRev
	 *            --base option, -1 if not set
	 * @param destRev
	 *            --dest option, -1 if not set
	 * @param collapse
	 *            true, if --collapse is to be used
	 * @param cont
	 *            true, if --continue is to be used
	 * @param abort
	 *            true, if --abort is to be used
	 * @param keepBranches
	 * @param useExternalMergeTool
	 * @param user
	 *            The user to use for collapse and continued collapse. May be null
	 * @return the output of the command
	 * @throws HgException
	 */
	public static String rebase(HgRoot hgRoot, int sourceRev, int baseRev, int destRev,
			boolean collapse, boolean cont, boolean abort, boolean keepBranches, boolean keep,
			boolean useExternalMergeTool, String user) throws HgException {
		AbstractShellCommand c = new HgCommand("rebase", "Rebasing", hgRoot, false);//$NON-NLS-1$
		c.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		c.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		if (!useExternalMergeTool) {
			// we use (non-existent) simplemerge, so no tool is started. We
			// need this option, though, as we still want the Mercurial merge to
			// take place.
			c.addOptions("--config", "ui.merge=simplemerge"); //$NON-NLS-1$ //$NON-NLS-2$

			// Do not invoke external editor for commit message
			// Future: Allow user to specify this
			c.addOptions("--config", "ui.editor=echo"); //$NON-NLS-1$ //$NON-NLS-2$
			// Future: Delete this block and use  addMergeToolPreference(command);
		}
		c.addOptions("--config", "extensions.hgext.rebase="); //$NON-NLS-1$ //$NON-NLS-2$

		// User is only applicable for collapse and continued collapse invocations
		if (user != null) {
			c.addOptions("--config", "ui.username=" + user); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!cont && !abort) {
			if (sourceRev >= 0 && baseRev <= 0) {
				c.addOptions("--source", "" + sourceRev); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (sourceRev < 0 && baseRev >= 0) {
				c.addOptions("--base", "" + baseRev); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (destRev >= 0) {
				c.addOptions("--dest", "" + destRev); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (collapse) {
				c.addOptions("--collapse"); //$NON-NLS-1$
			}
		}

		if (cont && !abort) {
			c.addOptions("--continue"); //$NON-NLS-1$
		}
		if (abort && !cont) {
			c.addOptions("--abort"); //$NON-NLS-1$
		}

		if (keepBranches) {
			c.addOptions("--keepbranches"); //$NON-NLS-1$
		}
		if (keep) {
			c.addOptions("--keep"); //$NON-NLS-1$
		}

		MercurialUtilities.setMergeViewDialogShown(false);

		return c.executeToString();
	}

	/**
	 * Invoke hg rebase --abort. Note: Refreshes the workspace.
	 *
	 * @param hgRoot
	 *            The hg root to use
	 * @return The result message
	 * @throws HgException
	 *             On error
	 */
	public static String abortRebase(HgRoot hgRoot) throws HgException {
		try {
			return rebase(hgRoot, -1, -1, -1, false, false, true, false, false, false, null);
		} finally {
			new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL).schedule();
		}
	}

	/**
	 * Check to see if we are in the middle of a rebase. <br/>
	 * Assume the presence of the <code>/.hg/rebasestate</code> file means that we are
	 *
	 * @param hgRoot
	 * @return <code>true</code> if we are currently rebasing
	 */
	public static boolean isRebasing(HgRoot hgRoot) {
		return new File(hgRoot, ".hg" + File.separator + "rebasestate").exists();
	}

	/**
	 * Determine if the given exception indicates a rebase conflict occurred.
	 * <p>
	 * Warning: Will this work on non-English locales?
	 * <p>
	 * Warning: Will hg output change?
	 *
	 * @param e
	 *            The exception to check
	 * @return True if the exception indicates a conflict occurred
	 */
	public static boolean isRebaseConflict(HgException e) {
		String message = e.getMessage();

		// Conflicts are expected:
		// 1.6.x:
		// /bin/sh: simplemerge: command not found
		// merging file1.txt
		// merging file1.txt
		// merging file1.txt failed!
		// abort: fix unresolved conflicts with hg resolve then run hg rebase --continue.
		// Command line: /home/john/runtime-New_configuration/hgtest2/hg -y
		// rebase --config ui.merge=simplemerge --config ui.editor=echo --config
		// extensions.hgext.rebase= --config ui.username=john --base 8 --dest 5, error
		// code: 255

		// 1.8.3 and 1.8.4
		// merging file1-4.txt
		// /bin/sh: simplemerge: command not found
		// merging file1-4.txt failed!
		// abort: unresolved conflicts (see hg resolve, then hg rebase --continue).
		// Command line: /home/john/runtime-New_configuration/hgtest:hg -y rebase --config
		// ui.merge=simplemerge --config ui.editor=echo --config extensions.hgext.rebase= --source
		// 3442 --dest 3441, error code: 255
		return (message != null && REBASING_CONFLICT.matcher(message).find());
	}
}
