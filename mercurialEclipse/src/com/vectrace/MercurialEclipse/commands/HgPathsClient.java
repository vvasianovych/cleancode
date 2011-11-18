/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - implementation
 *     Zsolt Koppany (Intland) - bug fixes
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import java.io.FileNotFoundException;
import java.util.Map;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgPathsClient extends AbstractClient {
	public static final String DEFAULT = Branch.DEFAULT;
	public static final String DEFAULT_PULL = "default-pull"; //$NON-NLS-1$
	public static final String DEFAULT_PUSH = "default-push"; //$NON-NLS-1$

	/**
	 * @param hgRoot non null
	 * @return map with "logical name" : "url" pairs. May be empty.
	 * @throws HgException
	 */
	public static Map<String, String> getPaths(HgRoot hgRoot) throws HgException {
		try {
			return hgRoot.getPaths();
		} catch (FileNotFoundException e) {
			throw new HgException("Unable to read paths for repository: " + hgRoot, e);
		}
	}
}
