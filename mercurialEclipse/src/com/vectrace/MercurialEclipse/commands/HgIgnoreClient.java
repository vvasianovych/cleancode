/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgIgnoreClient extends AbstractClient {

	public static void addExtension(IFile file) throws HgException {
		HgRoot hgRoot = getHgRoot(file);
		addPattern(hgRoot, "regexp", escape("." + file.getFileExtension()) + "$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static void addFile(IFile file) throws HgException {
		HgRoot hgRoot = getHgRoot(file);

		String regexp = "^" + getRelativePath(hgRoot, file) + "$"; //$NON-NLS-1$ //$NON-NLS-2$
		addPattern(hgRoot, "regexp", regexp); //$NON-NLS-1$
	}

	private static String getRelativePath(HgRoot hgRoot, IResource resource) {
		IPath path = ResourceUtils.getPath(resource);
		String relative = hgRoot.toRelative(path.toFile());
		relative = relative.replace('\\', '/');
		return escape(relative);
	}

	public static void addFolder(IFolder folder) throws HgException {
		HgRoot hgRoot = getHgRoot(folder);
		String regexp = "^" + getRelativePath(hgRoot, folder) + "$"; //$NON-NLS-1$ //$NON-NLS-2$
		addPattern(hgRoot, "regexp", regexp); //$NON-NLS-1$
	}

	public static void addRegexp(IProject project, String regexp) throws HgException {
		HgRoot hgRoot = getHgRoot(project);
		addPattern(hgRoot, "regexp", regexp); //$NON-NLS-1$
	}

	public static void addGlob(IProject project, String glob) throws HgException {
		HgRoot hgRoot = getHgRoot(project);
		addPattern(hgRoot, "glob", glob); //$NON-NLS-1$
	}

	private static String escape(String string) {
		StringBuilder result = new StringBuilder();
		int len = string.length();
		for(int i=0; i<len; i++) {
			char c = string.charAt(i);
			switch(c) {
				case '\\':
				case '.':
				case '*':
				case '?':
				case '+':
				case '|':
				case '^':
				case '$':
				case '(':
				case ')':
				case '[':
				case ']':
				case '{':
				case '}':
					result.append('\\');
			}
			result.append(c);
		}
		return result.toString();
	}

	private static void addPattern(HgRoot hgRoot, String syntax, String pattern) throws HgException {
		//TODO use existing sections
		BufferedOutputStream buffer = null;
		try {
			File hgignore = new File(hgRoot, ".hgignore");
			// append to file if it exists, else create a new one
			buffer = new BufferedOutputStream(new FileOutputStream(hgignore, true));
			// write contents
			buffer.write(new byte[] { '\n', 's', 'y', 'n', 't', 'a', 'x', ':', ' ' });
			buffer.write(syntax.getBytes());
			buffer.write('\n');
			buffer.write(pattern.getBytes());
			buffer.flush();
		} catch (IOException e) {
			throw new HgException(Messages.getString("HgIgnoreClient.failedToAddEntry"), e); //$NON-NLS-1$
		} finally {
			// we don't want to leak file descriptors...
			if (buffer != null) {
				try {
					buffer.close();
				} catch (IOException e) {
					throw new HgException(Messages
							.getString("HgIgnoreClient.failedToCloseHgIgnore"), e); //$NON-NLS-1$
				}
			}
		}

	}

}
