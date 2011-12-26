/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *     Andrei Loskutov  - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public class HgRollbackClient  extends AbstractClient {

	public static String rollback(final HgRoot hgRoot) throws CoreException {
		HgCommand command = new HgCommand("rollback", "Rolling back last transaction", hgRoot, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		String result = command.executeToString();
		new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL).schedule();
		return result;
	}

}
