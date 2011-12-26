/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch
 *     Andrei Loskutov           - bug fixes
 *     John Peberdy              - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class HgCommand extends AbstractShellCommand {

	private static final Set<String> COMMANDS_CONFLICTING_WITH_USER_ARG =
		Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
				"clone", "pull", "resolve", "showconfig", "status", "unbundle"
		)));

	private String lastUserName;

	private String bundleFile;

	// constructors

	public HgCommand(String command, String uiName, HgRoot root, boolean escapeFiles) {
		super(uiName, root, root, escapeFiles);
		this.command = command;

		Assert.isNotNull(command);
		Assert.isNotNull(hgRoot);
	}

	/**
	 * Invoke a hg command in the directory of the given resource using the resource to find the HgRoot.
	 *
	 * @param command The command to execute
	 * @param uiName Human readable name for this command
	 * @param resource The resource to use for working directory
	 * @param escapeFiles Whether to escape files
	 * @throws HgException
	 */
	public HgCommand(String command, String uiName, IResource resource, boolean escapeFiles) throws HgException {
		super(uiName, AbstractClient.getHgRoot(resource), ResourceUtils.getFirstExistingDirectory(ResourceUtils.getFileHandle(resource)), escapeFiles);
		this.command = command;

		Assert.isNotNull(command);
		Assert.isNotNull(hgRoot);
	}

	// operations

	public HgRoot getHgRoot() {
		return hgRoot;
	}

	/**
	 * <b>NOTE!</b> this method works only for hg commands which knows "-u" argument
	 * AND which understood "-u" as user name. There are commands which accept "-u" but
	 * threat is differently: like "resolve" or "status" (see {@link #isConflictingWithUserArg()}).
	 *
	 * @param user might be null or empty. In such case, a default user name weill be used.
	 * @throws IllegalArgumentException if the command uses "-u" NOT as user name parameter
	 */
	public void addUserName(String user) throws IllegalArgumentException {

		// avoid empty user
		user = user != null ? user : MercurialUtilities.getDefaultUserName();
		if(user != null) {
			user = user.trim();
			if (user.length() == 0) {
				user = null;
			} else {
				user = quote(user);
			}
		}
		if(user != null) {
			if (isConflictingWithUserArg()) {
				throw new IllegalArgumentException("Command '" + command
						+ "' uses '-u' argument NOT as user name!");
			}
			options.add("-u"); //$NON-NLS-1$
			options.add(user);
			this.lastUserName = user;
		} else {
			this.lastUserName = null;
		}
	}

	/**
	 * Remembers the user name given as option (see {@link #addUserName(String)}) as the default
	 * user name for current hg root. Should be called only after the command was successfully
	 * executed. If no hg root or no user name option was given, does nothing.
	 */
	public void rememberUserName(){
		if (lastUserName == null){
			return;
		}

		String commitName = HgCommitMessageManager.getDefaultCommitName(hgRoot);

		if(!commitName.equals(lastUserName)) {
			HgCommitMessageManager.setDefaultCommitName(hgRoot, lastUserName);
		}
	}

	private boolean isConflictingWithUserArg() {
		if(command == null){
			// TODO can it happen???
			return false;
		}
		return COMMANDS_CONFLICTING_WITH_USER_ARG.contains(command);
	}

	public void setBundleOverlay(File file) throws IOException {
		if (file != null) {
			bundleFile = file.getCanonicalPath();
		} else {
			bundleFile = null;
		}
	}

	/**
	 * @param str non null, non empty string
	 * @return non null string with escaped quotes (depending on the OS)
	 */
	private static String quote(String str) {
		if (!MercurialUtilities.isWindows()) {
			return str;
		}
		// escape quotes, otherwise commit will fail at least on windows
		return str.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see com.vectrace.MercurialEclipse.commands.AbstractShellCommand#customizeCommands(java.util.List)
	 */
	@Override
	protected void customizeCommands(List<String> cmd) {

		if (bundleFile != null) {
			// Add -R <bundleFile>
			cmd.add(1, bundleFile);
			cmd.add(1, "-R");
		}

		cmd.add(1, "-y");
	}

	/**
	 * @see com.vectrace.MercurialEclipse.commands.AbstractShellCommand#getExecutable()
	 */
	@Override
	protected String getExecutable() {
		return HgClients.getExecutable();
	}
}
