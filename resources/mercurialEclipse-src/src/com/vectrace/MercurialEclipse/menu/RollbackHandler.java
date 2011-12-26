/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.commands.HgRollbackClient;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class RollbackHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) throws CoreException {
		String result = HgRollbackClient.rollback(hgRoot);
		MessageDialog.openInformation(getShell(), Messages.getString("RollbackHandler.output"), result); //$NON-NLS-1$
	}

}
