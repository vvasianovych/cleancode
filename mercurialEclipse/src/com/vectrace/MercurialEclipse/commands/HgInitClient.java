/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		bastian	implementation
 * 		Andrei Loskutov - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public class HgInitClient extends AbstractClient {

	/**
	 * Creates hg repository at given location
	 * @param file non null directory (which may not exist yet)
	 */
	public static String init(File file) throws HgException {
		MercurialRootCache.getInstance().uncacheAllNegative();
		AbstractShellCommand command = new RootlessHgCommand("init", "Initializing repository", ResourceUtils
				.getFirstExistingDirectory(file));
		command.addOptions(file.getAbsolutePath());
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.DEFAULT_TIMEOUT);
		return command.executeToString();
	}

	/**
	 * Creates hg repository at given location
	 * @param repo non null repository (which may not exist yet)
	 */
	public static String init(IHgRepositoryLocation repo) throws HgException {
		MercurialRootCache.getInstance().uncacheAllNegative();
		AbstractShellCommand command = new RootlessHgCommand("init", "Initializing repository");
		if(repo.isLocal()) {
			command.addOptions(repo.getLocation());
		} else {
			command.addOptions(repo.getUri().toString());
		}
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PUSH_TIMEOUT);
		return command.executeToString();
	}
}
