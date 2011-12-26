/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Administrator	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.wizards.Messages;

public final class ClipboardUtils {

	private ClipboardUtils() {
		// hide constructor of utility class.
	}

	/**
	 * copy string to clipboard
	 *
	 * @param result
	 */
	public static void copyToClipboard(final String result) {
		if (result == null) {
			return;
		}
		if (MercurialEclipsePlugin.getStandardDisplay().getThread() == Thread
				.currentThread()) {
			Clipboard cb = new Clipboard(MercurialEclipsePlugin
					.getStandardDisplay());
			cb.setContents(new Object[] { result },
					new Transfer[] { TextTransfer.getInstance() });
			cb.dispose();
			return;
		}
		MercurialEclipsePlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				copyToClipboard(result);
			}
		});
	}

	/**
	 * @return string message from clipboard
	 */
	public static String getClipboardString() {
		if (MercurialEclipsePlugin.getStandardDisplay().getThread() == Thread
				.currentThread()) {
			Clipboard cb = new Clipboard(MercurialEclipsePlugin
					.getStandardDisplay());
			String result = (String) cb.getContents(TextTransfer.getInstance());
			cb.dispose();
			return result;
		}
		final String[] r = { null };
		MercurialEclipsePlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				r[0] = getClipboardString();
			}
		});
		return r[0];
	}

	public static File clipboardToTempFile(String prefix, String suffix)
			throws HgException {
		File file = null;
		String txt = getClipboardString();
		if (txt == null || txt.trim().length() == 0) {
			return null;
		}
		FileWriter w = null;
		try {
			file = File.createTempFile(prefix, suffix);
			w = new FileWriter(file);
			w.write(txt);
			return file;
		} catch (IOException e) {
			if (file != null && file.exists()) {
				boolean deleted = file.delete();
				if(!deleted){
					MercurialEclipsePlugin.logError("Failed to delete clipboard content file: " + file, null);
				}
			}
			throw new HgException(Messages
					.getString("ClipboardUtils.error.writeTempFile"), e); //$NON-NLS-1$
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
			}
		}
	}

	public static boolean isEmpty() {
		Clipboard cb = new Clipboard(MercurialEclipsePlugin
				.getStandardDisplay());
		String contents = (String) cb.getContents(TextTransfer.getInstance());
		cb.dispose();
		return contents == null || contents.trim().length() == 0;
	}
}
