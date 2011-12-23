/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.Assert;

/**
 * A command to invoke hg definitely outside of an hg root.
 */
public class RootlessHgCommand extends AbstractShellCommand {

	public RootlessHgCommand(String command, String uiName) {
		this(command, uiName, null);
	}

	public RootlessHgCommand(String command, String uiName, File workingDir) {
		super(uiName, null, workingDir, false);

		Assert.isNotNull(command);
		this.command = command;
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.commands.AbstractShellCommand#customizeCommands(java.util.List)
	 */
	@Override
	protected void customizeCommands(List<String> cmd) {
		cmd.add(1, "-y");
	}

	/**
	 * @see com.vectrace.MercurialEclipse.commands.AbstractShellCommand#getExecutable()
	 */
	@Override
	protected String getExecutable() {
		return HgClients.getExecutable();
	}
}
