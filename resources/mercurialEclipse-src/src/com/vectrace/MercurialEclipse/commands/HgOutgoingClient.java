/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bugfixes
 *     Philip Graf               - proxy support
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RemoteData;
import com.vectrace.MercurialEclipse.team.cache.RemoteKey;

public class HgOutgoingClient extends AbstractParseChangesetClient {

	/**
	 * @return never return null
	 */
	public static RemoteData getOutgoing(RemoteKey key) throws HgException {
		AbstractShellCommand command = getCommand(key);
		boolean computeFullStatus = MercurialEclipsePlugin.getDefault().getPreferenceStore().getBoolean(MercurialPreferenceConstants.SYNC_COMPUTE_FULL_REMOTE_FILE_STATUS);
		int style = computeFullStatus? AbstractParseChangesetClient.STYLE_WITH_FILES : AbstractParseChangesetClient.STYLE_WITH_FILES_FAST;
		addInsecurePreference(command);
		try {
			command.addOptions("--style", AbstractParseChangesetClient //$NON-NLS-1$
					.getStyleFile(style).getCanonicalPath());
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}

		if (key.isAllowUnrelated()) {
			command.addOptions("-f");
		}

		addRepoToHgCommand(key.getRepo(), command);

		String result = getResult(command);
		if (result == null) {
			return new RemoteData(key, Direction.OUTGOING);
		}

		RemoteData revisions = createRemoteRevisions(key, result, Direction.OUTGOING, null);
		return revisions;
	}

	private static String getResult(AbstractShellCommand command) throws HgException {
		try {
			String result = command.executeToString();
			if (result.endsWith("no changes found")) { //$NON-NLS-1$
				return null;
			}
			return result;
		} catch (HgException hg) {
			if (hg.getStatus().getCode() == 1) {
				return null;
			}
			throw hg;
		}
	}

	private static AbstractShellCommand getCommand(RemoteKey key) {
		HgRoot hgRoot = key.getRoot();
		AbstractShellCommand command = new HgCommand("outgoing", "Calculating outgoing changesets", hgRoot, false); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);

		// see issue 10495, 11093: there can be many branch heads: "--rev branch" cannot be used
		if (key.getBranch() != null) {
			command.addOptions("--branch", key.getBranch());
		}

		return command;
	}

}
