/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgRenameClient extends AbstractClient {

	public static void renameResource(IPath source, IPath dest, HgRoot hgRoot,
			IProgressMonitor monitor) throws HgException {
		if(!hgRoot.getIPath().isPrefixOf(source)) {
			throw new HgException("Cannot move: source ('" + source +
					"') is not inside the repository: '" + hgRoot + "'");
		}
		if(!hgRoot.getIPath().isPrefixOf(dest)) {
			throw new HgException("Cannot move: destination ('" + dest +
					"') is not inside the repository: '" + hgRoot + "'");
		}
		if (monitor != null) {
			monitor.subTask(Messages.getString("HgRenameClient.moving.1")
					+ source.lastSegment()
					+ Messages.getString("HgRenameClient.moving.2") //$NON-NLS-1$
					+ dest.lastSegment());
		}
		HgCommand command = new HgCommand("rename", "Renaming resource", hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.addOptions("--force"); //$NON-NLS-1$
		command.addFile(source.toFile());
		command.addFile(dest.toFile());
		command.executeToBytes();

		// see issue 14135: not versioned (new or derived) files may left after move
		// => move them manually (also allows "undo" in Eclipse to work properly)
		ResourceUtils.move(source.toFile(), dest.toFile());
	}
}
