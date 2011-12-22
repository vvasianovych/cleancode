/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian  implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.dialogs.BookmarkDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class BookmarkHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) {

		try {
			if (!MercurialUtilities.isCommandAvailable("bookmarks", //$NON-NLS-1$
					ResourceProperties.EXT_BOOKMARKS_AVAILABLE,
					"hgext.bookmarks=")) { //$NON-NLS-1$
				Shell shell = getShell();
				MessageDialog
						.openInformation(
								shell,
								Messages.getString("BookmarkHandler.extNotAvailable"), //$NON-NLS-1$
								Messages.getString("BookmarkHandler.extNotAvailableLong.1") //$NON-NLS-1$
										+ Messages.getString("BookmarkHandler.extNotAvailableLong.2") //$NON-NLS-1$
										+ Messages.getString("BookmarkHandler.extNotAvailableLong.3")); //$NON-NLS-1$
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}

		BookmarkDialog dialog = new BookmarkDialog(getShell(), hgRoot);
		dialog.open();
	}

}
