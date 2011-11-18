/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch  -  implementation
 * 		Andrei Loskutov  - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgDebugInstallClient extends AbstractClient {

	/**
	 * Key is the charset name, value is "true" if hg supports this charset encoding
	 */
	public static final Map<String, Boolean> ENCODINGS = Collections
			.synchronizedMap(new HashMap<String, Boolean>());

	public static String debugInstall() throws HgException {
		AbstractShellCommand command = getDebugInstallCommand();
		return new String(command.executeToBytes(Integer.MAX_VALUE)).trim();
	}

	private static AbstractShellCommand getDebugInstallCommand() {
		return new RootlessHgCommand("debuginstall", "Checking Mercurial installation") {
			{
				isInitialCommand = startSignal.getCount() > 0;
			}
		};
	}

	/**
	 * @param defaultCharset non null
	 * @return true if the given changeset is supported by the hg installation
	 */
	public static boolean hgSupportsEncoding(String defaultCharset) {
		Boolean b;
		synchronized(ENCODINGS){
			b = ENCODINGS.get(defaultCharset);
		}
		if (b == null) {
			AbstractShellCommand cmd = getDebugInstallCommand();
			cmd.addOptions("--encoding", defaultCharset); //$NON-NLS-1$
			try {
				cmd.executeToString();
			} catch (HgException e) {
				// there might have been an exception but it is not necessarily
				// related to the encoding. so only return false if the following
				// string is in the message
				if (e.getMessage().contains("unknown encoding:")) { //$NON-NLS-1$
					synchronized(ENCODINGS){
						ENCODINGS.put(defaultCharset, Boolean.FALSE);
					}
					return false;
				}
			}
			synchronized(ENCODINGS){
				ENCODINGS.put(defaultCharset, Boolean.TRUE);
			}
			return true;
		}
		return b.booleanValue();
	}
}
