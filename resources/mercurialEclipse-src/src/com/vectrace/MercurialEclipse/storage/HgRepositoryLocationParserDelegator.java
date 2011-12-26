/*******************************************************************************
 * Copyright (c) 2009 Intland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Berkes (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

/**
 * Delegates work of parsing persisted hg repository location to the
 * appropriate class/method
 *
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class HgRepositoryLocationParserDelegator {

	public IHgRepositoryLocation delegateParse(String line) {
		if (line != null) {
			if(line.startsWith(HgRepositoryLocationParser.PUSH_PREFIX) || line.startsWith(HgRepositoryLocationParser.PULL_PREFIX)) {
				return HgRepositoryLocationParser.parseLine(line);
			}
			try {
				return HgRepositoryLocationParser.parseLine(null, line, null, null);
			} catch (HgException ex) {
				MercurialEclipsePlugin.logError("Unable to parse repository line <" + line + ">", ex);
			}
		}
		return null;
	}

	public String delegateCreate(IHgRepositoryLocation location) {
		return HgRepositoryLocationParser.createLine(location);
	}
}
