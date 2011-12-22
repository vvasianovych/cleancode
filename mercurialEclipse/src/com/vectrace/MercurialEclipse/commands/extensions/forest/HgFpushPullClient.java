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
package com.vectrace.MercurialEclipse.commands.extensions.forest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.commands.RootlessHgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public class HgFpushPullClient extends HgPushPullClient {

	public static String fpush(File forestRoot, IHgRepositoryLocation repo,
			ChangeSet changeset, int timeout, File snapFile) throws CoreException {

		AbstractShellCommand command = new RootlessHgCommand("fpush", "Invoking fpush", forestRoot);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);
		if (snapFile != null) {
			try {
				command.addOptions("--snapfile", snapFile.getCanonicalPath());
			} catch (IOException e) {
				throw new HgException(e.getLocalizedMessage(), e);
			}
		}

		HgPushPullClient.applyChangeset(command, changeset);

		URI uri = repo.getUri();
		if (uri != null) {
			command.addOptions(uri.toASCIIString());
		} else {
			command.addOptions(repo.getLocation());
		}

		return new String(command.executeToBytes(timeout));
	}

	public static String fpull(File forestRoot, IHgRepositoryLocation repo,
			boolean update, boolean timeout, ChangeSet changeset,
			boolean walkHg, File snapFile, boolean partial) throws HgException {

		URI uri = repo.getUri();
		String pullSource;
		if (uri != null) {
			pullSource = uri.toASCIIString();
		} else {
			pullSource = repo.getLocation();
		}
		AbstractShellCommand command = new RootlessHgCommand("fpull", "Invoking fpull", forestRoot);

		if (update) {
			command.addOptions("--update");
		}

		applyChangeset(command, changeset);

		if (snapFile != null) {
			try {
				command.addOptions("--snapfile", snapFile.getCanonicalPath());
			} catch (IOException e) {
				throw new HgException(e.getLocalizedMessage(), e);
			}
		}

		if (walkHg) {
			command.addOptions("--walkhg", "true");
		}

		if (partial) {
			command.addOptions("--partial");
		}

		command.addOptions(pullSource);

		String result;
		if (timeout) {
			command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
			result = new String(command.executeToBytes());
		} else {
			result = new String(command.executeToBytes(Integer.MAX_VALUE));
		}
		Set<HgRoot> roots = MercurialEclipsePlugin.getRepoManager().getAllRepoLocationRoots(repo);
		// The reason to use "all" instead of only "local + incoming", is that we can pull
		// from another repo as the sync clients for given project may use
		// in this case, we also need to update "outgoing" changesets
		final int flags = RefreshRootJob.ALL;
		for (final HgRoot hgRoot : roots) {
			if (update) {
				new RefreshWorkspaceStatusJob(hgRoot, flags).schedule();
			} else {
				new RefreshRootJob(hgRoot, flags).schedule();
			}
		}
		return result;
	}
}
