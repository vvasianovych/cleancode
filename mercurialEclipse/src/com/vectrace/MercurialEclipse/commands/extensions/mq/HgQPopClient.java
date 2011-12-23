/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		bastian	implementation
 * 		Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 */
public class HgQPopClient extends AbstractClient {
	public static String popAll(IResource resource, boolean force)
			throws HgException {
		Assert.isNotNull(resource);
		AbstractShellCommand command = new HgCommand("qpop", //$NON-NLS-1$
				"Invoking qpop", resource, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		command.addOptions("-a"); //$NON-NLS-1$
		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}
		return command.executeToString();
	}

	public static String pop(IResource resource, boolean force, String patchName)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qpop", //$NON-NLS-1$
				"Invoking qpop", resource, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}

		if (!"".equals(patchName)) { //$NON-NLS-1$
			command.addOptions(patchName);
		}
		return command.executeToString();
	}
}
