/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.forest;

import java.io.File;
import java.net.URI;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.RootlessHgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author bastian
 *
 */
public class HgFcloneClient extends AbstractClient {
	public static void fclone(File parentDirectory, IHgRepositoryLocation repo,
			boolean noUpdate, boolean pull, boolean uncompressed,
			boolean timeout, String rev, String cloneName)
			throws HgException {
		AbstractShellCommand command = new RootlessHgCommand("fclone", "Invoking fclone", parentDirectory); //$NON-NLS-1$

		//        command.addOptions("--config", "extensions.hgext.forest="); //$NON-NLS-1$ //$NON-NLS-2$

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

		URI uri = repo.getUri();
		if (uri != null) {
			command.addOptions(uri.toASCIIString());
		} else {
			command.addOptions(repo.getLocation());
		}

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
