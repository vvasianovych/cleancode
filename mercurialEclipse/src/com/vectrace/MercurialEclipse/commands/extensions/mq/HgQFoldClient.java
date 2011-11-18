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

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgCommitClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Patch;

/**
 * @author bastian
 *
 */
public class HgQFoldClient extends AbstractClient {
	public static String fold(IResource resource, boolean keep, String message,
			List<Patch> patches) throws HgException {
		Assert.isNotNull(patches);
		Assert.isNotNull(resource);
		HgCommand command = new HgCommand("qfold", //$NON-NLS-1$
				"Invoking qfold", resource, true);
		File messageFile = null;

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		if (keep) {
			command.addOptions("--keep"); //$NON-NLS-1$
		}
		if (message != null && message.length() > 0) {
			messageFile = HgCommitClient.addMessage(command, message);
		}

		for (Patch patch : patches) {
			command.addOptions(patch.getName());
		}

		try {
			return command.executeToString();
		} finally {
			HgCommitClient.deleteMessage(messageFile);
		}
	}
}
