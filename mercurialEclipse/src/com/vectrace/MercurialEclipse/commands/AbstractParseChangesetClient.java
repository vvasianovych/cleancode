/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch	      - implementation
 * 		Andrei Loskutov       - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RemoteData;
import com.vectrace.MercurialEclipse.team.cache.RemoteKey;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This class helps HgClients to parse the changeset output of hg to Changeset objects.
 *
 * @author Bastian Doetsch
 */
public abstract class AbstractParseChangesetClient extends AbstractClient {

	private static final String OPEN_TAG = "<c>";
	private static final String CLOSE_TAG = "</c>";
	private static final int OPEN_CLOSE_TAG_SIZE = OPEN_TAG.length() + CLOSE_TAG.length();
	private static final String STYLES = "/styles";
	private static final String DEFAULT = "/log_style"; //$NON-NLS-1$
	private static final String WITH_FILES = "/log_style_with_files"; //$NON-NLS-1$
	private static final String WITH_FILES_FAST = "/log_style_fast"; //$NON-NLS-1$
	private static final String TEMP_EXTN = ".tmpl"; //$NON-NLS-1$
	public static final int STYLE_DEFAULT = 0;
	public static final int STYLE_WITH_FILES = 1;
	public static final int STYLE_WITH_FILES_FAST = 2;

	/**
	 * Return a File reference to a copy of the required mercurial style file.
	 * Two types are available, one that includes the files and one that
	 * doesn't. Using the one with files can be very slow on large repos.
	 * <p>
	 * These style files are included in the plugin jar file and need to be
	 * copied out of there into the plugin state area so a path can be given to
	 * the hg command.
	 *
	 * @param styleBit
	 *            return one of the STYLE_ constants
	 * @return a File reference to an existing file
	 */
	public static File getStyleFile(int styleBit) throws HgException {
		String style;

		switch (styleBit) {
		case STYLE_WITH_FILES:
			style = WITH_FILES;
			break;
		case STYLE_WITH_FILES_FAST:
			style = WITH_FILES_FAST;
			break;
		default:
			style = DEFAULT;
			break;
		}
		String styleTemplate = style + TEMP_EXTN;

		IPath sl = MercurialEclipsePlugin.getDefault().getStateLocation();

		File stylefile = sl.append(style).toFile();
		File tmplfile = sl.append(styleTemplate).toFile();

		if (stylefile.canRead() && tmplfile.canRead()) {
			// Already have copies, return the file reference to the style file
			stylefile.deleteOnExit();
			tmplfile.deleteOnExit();
			return stylefile;
		}

		ClassLoader cl = AbstractParseChangesetClient.class.getClassLoader();
		// Need to make copies into the state directory from the jar file.
		// set delete on exit so a new copy is made each time eclipse is started
		// so we don't use stale copies on plugin updates.
		InputStream styleistr = cl.getResourceAsStream(STYLES + style);
		InputStream tmplistr = cl.getResourceAsStream(STYLES + styleTemplate);
		OutputStream styleostr = null;
		OutputStream tmplostr = null;
		try {
			styleostr = new FileOutputStream(stylefile);
			tmplostr = new FileOutputStream(tmplfile);
			tmplfile.deleteOnExit();

			byte[] buffer = new byte[1024];
			int n;
			while ((n = styleistr.read(buffer)) != -1) {
				styleostr.write(buffer, 0, n);
			}
			while ((n = tmplistr.read(buffer)) != -1) {
				tmplostr.write(buffer, 0, n);
			}
			return stylefile;
		} catch (IOException e) {
			throw new HgException("Failed to setup hg style file", e); //$NON-NLS-1$
		} finally {
			try {
				if(styleostr != null) {
					styleostr.close();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
			try {
				if(tmplostr != null) {
					tmplostr.close();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	/**
	 * Parse log output into a set of changesets.
	 * <p>
	 * Format of input is defined in the two style files in /styles directory
	 *
	 * @param input
	 *            output from the hg log command
	 * @param direction
	 *            Incoming, Outgoing or Local changesets
	 * @param repository
	 * @param bundleFile
	 * @return map where the key is an absolute file path, never null
	 * @throws HgException
	 */
	protected static Map<IPath, Set<ChangeSet>> createLocalRevisions(
			IResource res, String input,
			Direction direction, IHgRepositoryLocation repository,
			File bundleFile, String branch) throws HgException {

		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(res);
		IPath path = ResourceUtils.getPath(res);

		return createLocalRevisions(path, input, direction, repository, bundleFile, branch, hgRoot);
	}

	protected static RemoteData createRemoteRevisions(RemoteKey key, String input,
			Direction direction, File bundleFile)
			throws HgException {
		if (input == null || input.length() == 0){
			return new RemoteData(key, direction);
		}

		IPath path = new Path(key.getRoot().getAbsolutePath());
		ChangesetContentHandler xmlHandler = parseInput(path, false, input, direction, key.getRepo(),
				bundleFile, key.getBranch(), key.getRoot());
		return xmlHandler.createRemoteData();
	}

	/**
	 * @param path full absolute file path, which MAY NOT EXIST in the local file system
	 *        (because it is the original path of moved or renamed file)
	 *
	 * @return map where the key is an absolute file path, never null
	 * @throws HgException
	 */
	protected static Map<IPath, Set<ChangeSet>> createLocalRevisions(IPath path, String input,
			Direction direction, IHgRepositoryLocation repository, File bundleFile, String branch, HgRoot hgRoot)
			throws HgException {
		Map<IPath, Set<ChangeSet>> fileRevisions = new HashMap<IPath, Set<ChangeSet>>();

		if (input == null || input.length() == 0 || hgRoot == null) {
			return fileRevisions;
		}
		ChangesetContentHandler xmlHandler = parseInput(path, true, input, direction, repository,
				bundleFile, branch, hgRoot);
		return xmlHandler.getFileRevisions();
	}

	private static ChangesetContentHandler parseInput(IPath path, boolean withFiles, String input,
			Direction direction, IHgRepositoryLocation repository, File bundleFile, String branch, HgRoot hgRoot)
			throws HgException {
		StringBuilder sb = new StringBuilder(input.length() + OPEN_CLOSE_TAG_SIZE);
		sb.append(OPEN_TAG);
		sb.append(input);
		sb.append(CLOSE_TAG);
		String myInput = sb.toString();

		ChangesetContentHandler xmlHandler = new ChangesetContentHandler(path, withFiles, direction, repository,
				bundleFile, branch, hgRoot);
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(xmlHandler);
			reader.parse(new InputSource(new StringReader(myInput)));
		} catch (Exception e) {
			String nextTry = cleanControlChars(myInput);
			try {
				XMLReader reader = XMLReaderFactory.createXMLReader();
				reader.setContentHandler(xmlHandler);
				reader.parse(new InputSource(new StringReader(nextTry)));
			} catch (Exception e1) {
				MercurialEclipsePlugin.logError(e);
				throw new HgException(e1.getLocalizedMessage(), e1);
			}
		}
		return xmlHandler;
	}

	/**
	 * Clean the string of special chars that might be invalid for the XML
	 * parser. Return the cleaned string (special chars replaced by ordinary
	 * spaces).
	 *
	 * @param str
	 *            the string to clean
	 * @return the cleaned string
	 */
	private static String cleanControlChars(String str) {
		final StringBuilder buf = new StringBuilder();
		final int len = str.length();
		boolean inString=false;
		for (int i = 0; i < len; i++) {
			final int ch = str.codePointAt(i);
			if (ch == '\r' || ch == '\n' || ch == '\t') {
				buf.appendCodePoint(ch);
			} else if (Character.isISOControl(ch)) {
				buf.append(' ');
			} else if (ch == '&') {
				buf.append("&amp;");
			} else if (ch == '"') {
				buf.append("\"");
				inString = !inString;
			} else if ((ch == '<') && inString) {
				buf.append("&lt;");
			} else if ((ch == '>') && inString) {
				buf.append("&gt;");
			} else {
				buf.appendCodePoint(ch);
			}
		}
		return buf.toString();
	}
}
