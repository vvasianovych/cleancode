/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch 			- implementation of remove
 *     	Andrei Loskutov         	- bug fixes
 *     	Zsolt Koppany (Intland)		- bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vectrace.MercurialEclipse.compare.TagComparator;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;

public class HgTagClient extends AbstractClient {
	private static final Pattern GET_TAGS_PATTERN = Pattern.compile("^(.*) ([-0-9]+):([a-f0-9]+)( local)?$"); //$NON-NLS-1$

	/**
	 * Fetches all tags for given root. The tags do NOT have full changeset info
	 * attached.
	 * @param hgRoot non null
	 * @return never null, might be empty array
	 * @throws HgException
	 */
	public static Tag[] getTags(HgRoot hgRoot) throws HgException {
		AbstractShellCommand command = new HgCommand("tags", "Retrieving tags", hgRoot, false); //$NON-NLS-1$
		command.addOptions("-v"); //$NON-NLS-1$
		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$

		Collection<Tag> tags = getTags(hgRoot, lines);
		Tag[] sortedTags = tags.toArray(new Tag[] {});
		return sortedTags;
	}

	/**
	 * @param hgRoot non null
	 * @param withChangesets true to fetch corresponding changesets too
	 * @return never null, might be empty array
	 * @throws HgException
	 */
	public static Tag[] getTags(HgRoot hgRoot, boolean withChangesets) throws HgException {
		Tag[] tags = getTags(hgRoot);
		if(withChangesets) {
			for (Tag tag : tags) {
				// triggers changeset loading, if the local changeset cache
				// doesn't contain the tag version
				tag.getChangeSet();
			}
		}
		return tags;
	}

	protected static Collection<Tag> getTags(HgRoot hgRoot, String[] lines) throws HgException {
		List<Tag> tags = new ArrayList<Tag>();

		for (String line : lines) {
			Matcher m = GET_TAGS_PATTERN.matcher(line);
			if (m.matches()) {
				String tagName = m.group(1).trim();
				Tag tag = new Tag(hgRoot, tagName, Integer.parseInt(m.group(2)), m.group(3), m.group(4) != null);
				tags.add(tag);
			} else {
				throw new HgException(Messages.getString("HgTagClient.parseException") + line + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		Collections.sort(tags, new TagComparator());
		return tags;
	}

	/**
	 * @param user
	 *            if null, uses the default user
	 * @throws HgException
	 */
	public static void addTag(HgRoot hgRoot, String name, String rev, String user, boolean local, boolean force) throws HgException {
		HgCommand command = new HgCommand(
				"tag", "Tagging revision " + ((rev == null) ? "" : rev + " ") + "as " + name, hgRoot, false); //$NON-NLS-1$
		if (local) {
			command.addOptions("-l");
		}
		if (force) {
			command.addOptions("-f");
		}
		if (rev != null) {
			command.addOptions("-r", rev);
		}
		command.addUserName(user);
		command.addOptions(name);
		command.executeToBytes();
		command.rememberUserName();
	}

	public static String removeTag(HgRoot hgRoot, Tag tag, String user) throws HgException {
		HgCommand command = new HgCommand("tag", "Removing tag " + tag, hgRoot, false); //$NON-NLS-1$
		command.addUserName(user);
		command.addOptions("--remove");
		command.addOptions(tag.getName());
		String result = command.executeToString();
		command.rememberUserName();
		return result;
	}
}
