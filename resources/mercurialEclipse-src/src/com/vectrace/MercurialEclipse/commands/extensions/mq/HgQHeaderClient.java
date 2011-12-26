/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Philip Graf    implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * Client for {@code qheader}.
 *
 * @author Philip Graf
 */
public class HgQHeaderClient extends AbstractClient {

	/**
	 * Returns the header of the topmost patch. This method calls {@code hg qheader} without a
	 * specified patch and returns the result.
	 *
	 * @param resource
	 * @return The header of the topmost patch. Never returns {@code null}.
	 * @throws HgException
	 *             Thrown when the Hg command cannot be executed.
	 */
	public static String getHeader(HgRoot root) throws HgException {
		AbstractShellCommand command = new HgCommand("qheader", "Invoking qheader", root, false); //$NON-NLS-1$
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$
		return command.executeToString().trim();
	}

}
