/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgParentClient extends AbstractClient {

	private static final Pattern ANCESTOR_PATTERN = Pattern
			.compile("^(-?[0-9]+):([0-9a-f]+)$"); //$NON-NLS-1$

	private static final Pattern LINE_SEPERATOR_PATTERN = Pattern.compile("\n");

	public static int[] getParents(HgRoot hgRoot) throws HgException {
		AbstractShellCommand command = new HgCommand("parents", //$NON-NLS-1$
				"Finding parent revisions", hgRoot, false);
		command.addOptions("--template", "{rev}\n"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] lines = getLines(command.executeToString());
		int[] parents = new int[lines.length];
		for (int i = 0; i < lines.length; i++) {
			parents[i] = Integer.parseInt(lines[i]);
		}
		return parents;
	}

	public static String[] getParentNodeIds(HgRoot hgRoot)
			throws HgException {
		AbstractShellCommand command = new HgCommand("parents", "Finding parent revisions", hgRoot,
				false);
		command.addOptions("--template", "{node}\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return parseParentsCommand(command);
	}

	public static String[] getParentNodeIds(IResource file)
	throws HgException {
		AbstractShellCommand command = new HgCommand("parents", //$NON-NLS-1$
				"Finding parent revisions", file, false);
		if(file instanceof IFile) {
			command.addFiles(file);
		}
		command.addOptions("--template", "{node}\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return parseParentsCommand(command);
	}

	public static String[] getParentNodeIds(ChangeSet cs, String template) throws HgException {
		AbstractShellCommand command = new HgCommand("parents", //$NON-NLS-1$
				"Finding parent revisions", cs.getHgRoot(), false);
		command.addOptions("--template", template + "\n", "--rev", cs //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				.getChangeset());
		return parseParentsCommand(command);
	}

	public static String[] getParentNodeIds(IResource resource, ChangeSet cs) throws HgException {
		AbstractShellCommand command = new HgCommand("parents", //$NON-NLS-1$
				"Finding parent revisions", resource, false);
		command.addOptions("--template", "{node}\n", "--rev", cs //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				.getChangeset());
		return parseParentsCommand(command);
	}

	private static String[] parseParentsCommand(AbstractShellCommand parentsCommand) throws HgException {
		String[] lines = getLines(parentsCommand.executeToString());
		String[] parents = new String[lines.length];
		for (int i = 0; i < lines.length; i++) {
			parents[i] = lines[i].trim();
		}
		return parents;
	}

	/**
	 * @return The revision index or -1 for unrelated
	 * @throws HgException
	 */
	public static int findCommonAncestor(HgRoot hgRoot, String node1, String node2)
			throws HgException {
		AbstractShellCommand command = new HgCommand("debugancestor", //$NON-NLS-1$
				"Finding common ancestor", hgRoot, false);
		command.addOptions(node1, node2);
		String result = command.executeToString().trim();
		Matcher m = ANCESTOR_PATTERN.matcher(result);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		}
		throw new HgException("Parse exception: '" + result + "'");
	}

	/**
	 * This methods finds the common ancestor of two changesets, supporting
	 * overlays for using incoming changesets. Only one changeset may be
	 * incoming.
	 *
	 * @param hgRoot
	 *            hg root
	 * @param cs1
	 *            first changeset
	 * @param cs2
	 *            second changeset
	 * @return the id of the ancestor
	 * @throws HgException
	 */
	public static int findCommonAncestor(HgRoot hgRoot, ChangeSet cs1, ChangeSet cs2)
			throws HgException {
		String result;
		try {
			HgCommand command = new HgCommand("debugancestor", "Finding common ancestor", hgRoot,
					false);

			if (cs1.getBundleFile() != null || cs2.getBundleFile() != null) {
				if (cs1.getBundleFile() != null) {
					command.setBundleOverlay(cs1.getBundleFile());
				} else {
					command.setBundleOverlay(cs2.getBundleFile());
				}
			}

			command.addOptions(cs1.getChangeset(), cs2.getChangeset());

			result = command.executeToString().trim();
			Matcher m = ANCESTOR_PATTERN.matcher(result);
			if (m.matches()) {
				return Integer.parseInt(m.group(1));
			}
			throw new HgException("Parse exception: '" + result + "'");
		} catch (NumberFormatException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Splits an output of a command into lines. Lines are separated by a newline character (\n).
	 *
	 * @param output
	 *            The output of a command.
	 * @return The lines of the output.
	 */
	private static String[] getLines(String output) {
		if (output == null || output.length() == 0) {
			return new String[0];
		}
		return LINE_SEPERATOR_PATTERN.split(output);
	}
}
