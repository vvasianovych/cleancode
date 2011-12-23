/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *     Philip Graf               - proxy support
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgCloneClient extends AbstractClient {

	public static void clone(File parentDirectory, IHgRepositoryLocation repo,
			boolean noUpdate, boolean pull, boolean uncompressed,
			boolean timeout, String rev, String cloneName) throws HgException {
		AbstractShellCommand command = new RootlessHgCommand("clone", "Cloning", parentDirectory); //$NON-NLS-1$

		addInsecurePreference(command);
		if (noUpdate) {
			command.addOptions("--noupdate"); //$NON-NLS-1$
		}
		if (pull) {
			command.addOptions("--pull"); //$NON-NLS-1$
		}
		if (uncompressed) {
			command.addOptions("--uncompressed"); //$NON-NLS-1$
		}
		if (rev != null && rev.length() > 0) {
			command.addOptions("--rev", rev); //$NON-NLS-1$
		}

		addRepoToHgCommand(repo, command);

		if (cloneName != null) {
			command.addOptions(cloneName);
		}
		if (timeout) {
			command
					.setUsePreferenceTimeout(MercurialPreferenceConstants.CLONE_TIMEOUT);
			command.executeToBytes();
		} else {
			command.executeToBytes(Integer.MAX_VALUE);
		}
	}
}
