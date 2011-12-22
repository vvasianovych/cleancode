/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Jerome Negre		- 	Initial implementation
 * 		Bastian Doetsch	-	implemented some safeguards for the ok button
 *     	Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.TagDialog;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class TagHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) throws CoreException {
		TagDialog dialog = new TagDialog(getShell(), hgRoot);

		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}
		String name = dialog.getName();
		if (name != null && name.trim().length() > 0) {
			HgTagClient.addTag(hgRoot, name.trim(), dialog.getTargetRevision(), dialog.getUser(), dialog
					.isLocal(), dialog.isForced());
			new RefreshRootJob(
					Messages.getString("TagHandler.refreshing"), hgRoot, RefreshRootJob.LOCAL_AND_OUTGOING).schedule(); //$NON-NLS-1$
		}
	}

}
