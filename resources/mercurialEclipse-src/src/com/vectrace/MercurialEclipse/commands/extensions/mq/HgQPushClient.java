/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class HgQPushClient extends AbstractClient {
	public static String pushAll(IResource resource, boolean force)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qpush", //$NON-NLS-1$
				"Invoking qpush", resource, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		command.addOptions("-a"); //$NON-NLS-1$
		if (force) {
			command.addOptions("--force"); //$NON-NLS-1$
		}
		return command.executeToString();
	}

	public static String push(IResource resource, boolean force, String patchName)
			throws HgException {
		AbstractShellCommand command = new HgCommand("qpush", //$NON-NLS-1$
				"Invoking qpush", resource, true);

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
