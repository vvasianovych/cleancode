/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		bastian	implementation
 * 		Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Bookmark;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
public class HgBookmarkClient extends AbstractClient {

	/**
	 * @return a List of bookmarks
	 */
	public static List<Bookmark> getBookmarks(HgRoot hgRoot) throws HgException {
		AbstractShellCommand cmd = new HgCommand("bookmarks", "Listing bookmarks", hgRoot, true);
		cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
		String result = cmd.executeToString();
		return convert(result);
	}

	private static ArrayList<Bookmark> convert(String result) {
		ArrayList<Bookmark> bookmarks = new ArrayList<Bookmark>();
		if (!result.startsWith("no bookmarks set")) { //$NON-NLS-1$
			String[] split = result.split("\n"); //$NON-NLS-1$
			for (String string : split) {
				bookmarks.add(new Bookmark(string));
			}
		}
		return bookmarks;
	}

	public static String create(HgRoot hgRoot, String name, String targetChangeset)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("bookmarks", "Adding bookmark", hgRoot, true);
		cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
		cmd.addOptions("--rev", targetChangeset, name); //$NON-NLS-1$
		return cmd.executeToString();
	}

	public static String rename(HgRoot hgRoot, String name, String newName)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("bookmarks", "Renaming bookmark", hgRoot, true);
		cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
		cmd.addOptions("--rename", name, newName); //$NON-NLS-1$
		return cmd.executeToString();
	}

	public static String delete(HgRoot hgRoot, String name)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("bookmarks", "Deleting bookmark", hgRoot, true);
		cmd.addOptions("--config", "extensions.hgext.bookmarks="); //$NON-NLS-1$ //$NON-NLS-2$
		cmd.addOptions("--delete", name); //$NON-NLS-1$
		return cmd.executeToString();
	}

}
