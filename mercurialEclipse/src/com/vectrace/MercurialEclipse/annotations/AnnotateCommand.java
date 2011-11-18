/*******************************************************************************
 * Copyright (c) Subclipse and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     StefanC           - remove empty lines, code cleenup
 *     Jérôme Nègre      - make it work
 *     Bastian Doetsch   - refactorings
 *     Andrei Loskutov   - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.annotations;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class AnnotateCommand {
	private static final Pattern ANNOTATE = Pattern
			.compile("^\\s*(.+?)\\s+(\\d+)\\s+(\\w+)\\s+(\\w+ \\w+ \\d+ \\d+:\\d+:\\d+ \\d+ [\\+\\-]\\d+)"); //$NON-NLS-1$

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH); //$NON-NLS-1$

	private final IResource file;

	public AnnotateCommand(IResource remoteFile) {
		this.file = remoteFile;
	}

	public AnnotateBlocks execute() throws HgException {

		if (!MercurialTeamProvider.isHgTeamProviderFor(file)) {
			return null;
		}

		HgCommand command = new HgCommand("annotate", "Fetching annotations for resource", file, true);

		command.addOptions("--follow", "--user", "--number", "--changeset", "--date");
		command.addFiles(file);

		return createFromStdOut(new StringReader(command.executeToString()));
	}

	protected static AnnotateBlocks createFromStdOut(InputStream contents) {
		return createFromStdOut(new InputStreamReader(contents));
	}

	protected static synchronized AnnotateBlocks createFromStdOut(Reader contents) {
		AnnotateBlocks blocks = new AnnotateBlocks();
		try {
			BufferedReader reader = new BufferedReader(contents);
			int count = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher matcher = ANNOTATE.matcher(line);
				if (!matcher.find()) {
					// ignore empty lines
					continue;
				}
				String author = matcher.group(1);
				int revision = Integer.parseInt(matcher.group(2));
				String changeset = matcher.group(3);
				Date date = DATE_FORMAT.parse(matcher.group(4));
				blocks.add(new AnnotateBlock(
						new HgRevision(changeset, revision), author, date,
						count, count));
				count++;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return blocks;
	}
}
