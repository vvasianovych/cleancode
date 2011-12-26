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

import org.eclipse.core.runtime.Assert;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQFinishClient extends AbstractClient {
	private static AbstractShellCommand makeCommand(HgRoot root) {
		Assert.isNotNull(root);
		AbstractShellCommand command = new HgCommand("qfinish", //$NON-NLS-1$
				"Invoking qfinish", root, true);
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		return command;
	}

	public static String finish(HgRoot root, String rev) throws HgException {
		Assert.isNotNull(rev);
		AbstractShellCommand command = makeCommand(root);
		command.addOptions(rev);
		return command.executeToString();
	}

	/**
	 * Calls qfinish -a
	 */
	public static String finishAllApplied(HgRoot root) throws HgException {
		AbstractShellCommand command = makeCommand(root);
		command.addOptions("--applied");

		return command.executeToString();
	}
}
