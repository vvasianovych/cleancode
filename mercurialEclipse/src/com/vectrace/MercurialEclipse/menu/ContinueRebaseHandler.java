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
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.dialogs.ContinueRebaseDialog;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class ContinueRebaseHandler extends RunnableHandler {

	@Override
	public void run(HgRoot hgRoot) throws CoreException {
		CommitDialog commitDialog = new ContinueRebaseDialog(getShell(), hgRoot);

		// open dialog and wait for ok
		commitDialog.open();
	}

}
