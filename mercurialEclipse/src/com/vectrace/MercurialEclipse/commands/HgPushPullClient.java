/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - added authentication to push
 *     Andrei Loskutov           - bug fixes
 *     Ilya Ivanov (Intland) 	 - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.regex.Pattern;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.UpdateJob;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public class HgPushPullClient extends AbstractClient {

	/**
	 * matches ("number" "heads") message
	 */
	private static final Pattern HEADS_PATTERN = Pattern.compile("\\(\\+\\d+\\sheads\\)");

	public static String push(HgRoot hgRoot, IHgRepositoryLocation repo,
			boolean force, ChangeSet changeset, int timeout) throws HgException {
		return push(hgRoot, repo, force, changeset, timeout, null);
	}

	public static String push(HgRoot hgRoot, IHgRepositoryLocation repo,
			boolean force, ChangeSet changeset, int timeout, String branch) throws HgException {
		AbstractShellCommand command = new HgCommand("push", //$NON-NLS-1$
				makeDescription("Pushing", changeset, branch), hgRoot, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);

		addInsecurePreference(command);

		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}

		applyChangeset(command, changeset);

		boolean newBranch = MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getBoolean(MercurialPreferenceConstants.PREF_PUSH_NEW_BRANCH);

		if (newBranch) {
			command.addOptions("--new-branch");
		}

		if (branch != null) {
			command.addOptions("--branch", branch);
		}

		addRepoToHgCommand(repo, command);
		return new String(command.executeToBytes(timeout));
	}

	public static String pull(HgRoot hgRoot, ChangeSet changeset,
			IHgRepositoryLocation repo, boolean update, boolean rebase,
			boolean force, boolean timeout, boolean merge) throws HgException {
		return pull(hgRoot, changeset, repo, update, rebase, force, timeout, merge, null);
	}

	public static String pull(HgRoot hgRoot, ChangeSet changeset,
			IHgRepositoryLocation repo, boolean update, boolean rebase,
			boolean force, boolean timeout, boolean merge, String branch) throws HgException {

		HgCommand command = new HgCommand("pull", //$NON-NLS-1$
				makeDescription("Pulling", changeset, branch), hgRoot, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));

		addInsecurePreference(command);

		if (update) {
			command.addOptions("--update"); //$NON-NLS-1$
			addMergeToolPreference(command);
		} else if (rebase) {
			command.addOptions("--config", "extensions.hgext.rebase="); //$NON-NLS-1$ //$NON-NLS-2$
			command.addOptions("--rebase"); //$NON-NLS-1$
			addMergeToolPreference(command);
		}

		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}

		applyChangeset(command, changeset);

		if (branch != null) {
			command.addOptions("--branch", branch);
		}

		addRepoToHgCommand(repo, command);

		String result = null;
		try {
			if (timeout) {
				command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
				result = new String(command.executeToBytes());
			} else {
				result = new String(command.executeToBytes(Integer.MAX_VALUE));
			}
		} finally {
			if (update && result != null && !merge && !rebase) {
				// different messages from hg depending on if branch was set or not
				if(result.contains("not updating, since new heads added") ||
						(branch != null &&
								HEADS_PATTERN.matcher(result).find())){

				// inform user about new heads and ask if he wants to merge or rebase
				UpdateJob.handleMultipleHeads(hgRoot, false);
			}
			}

			// doesn't matter how far we were: we have to trigger update of caches in case
			// the pull was *partly* successful (e.g. pull was ok, but update not)
			refreshProjects(update, hgRoot);
		}
		return result;
	}

	private static String makeDescription(String op, ChangeSet changeset, String branch) {
		if (changeset == null) {
			return op + " all changes" + ((branch == null) ? "" : " in " + branch);
		}

		return op + " changeset " + changeset.getChangeset();
	}

	protected static void applyChangeset(AbstractShellCommand command, ChangeSet changeset) {
		if (changeset != null) {
			String cs = changeset.getChangeset();

			if (cs != null && (cs = cs.trim()).length() > 0) {
				command.addOptions("-r", cs); //$NON-NLS-1$
			}
		}
	}

	private static void refreshProjects(boolean update, final HgRoot hgRoot) {
		// The reason to use "all" instead of only "local + incoming", is that we can pull
		// from another repo as the sync clients for given project may use
		// in this case, we also need to update "outgoing" changesets
		final int flags = RefreshRootJob.ALL;
		if(update) {
			new RefreshWorkspaceStatusJob(hgRoot, flags).schedule();
		} else {
			new RefreshRootJob(hgRoot, flags).schedule();
		}
	}
}
