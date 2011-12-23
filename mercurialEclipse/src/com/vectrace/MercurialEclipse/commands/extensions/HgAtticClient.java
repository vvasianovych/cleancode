/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgAtticClient extends AbstractClient {

	public static String shelve(HgRoot hgRoot, String commitMessage,
			boolean git, String user, String name) throws HgException {
		HgCommand cmd = new HgCommand("attic-shelve", "Invoking attic-shelve", hgRoot, false);

		if (commitMessage != null && commitMessage.length() > 0) {
			cmd.addOptions("-m", commitMessage); // $NON-NLS-1$
		}

		if (git) {
			cmd.addOptions("--git"); // $NON-NLS-1$
		}

		cmd.addUserName(user);

		cmd.addOptions("--currentdate", name); // $NON-NLS-1$
		String result = cmd.executeToString();
		cmd.rememberUserName();
		return result;
	}

	public static String unshelve(HgRoot hgRoot, boolean guessRenamedFiles,
			boolean delete, String name) throws HgException {
		AbstractShellCommand cmd = new HgCommand("attic-unshelve", "Invoking attic-unshelve", hgRoot, false);

		if (guessRenamedFiles) {
			cmd.addOptions("--similarity"); // $NON-NLS-1$
		}

		if (delete) {
			cmd.addOptions("--delete"); // $NON-NLS-1$
		}

		cmd.addOptions(name);
		return cmd.executeToString();
	}

}
