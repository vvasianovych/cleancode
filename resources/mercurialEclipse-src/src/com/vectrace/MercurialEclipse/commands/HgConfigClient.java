/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class HgConfigClient extends AbstractClient {
	public static String getHgConfigLine(String key)
			throws HgException {
		String[] lines = getHgConfigLines(key);
		return lines[0];
	}

	private static String[] getHgConfigLines(String key)
			throws HgException {
		AbstractShellCommand cmd = new RootlessHgCommand("showconfig", "Getting configuration information");
		cmd.addOptions(key);
		String[] lines = cmd.executeToString().split("\n"); //$NON-NLS-1$
		return lines;
	}
}
