/*******************************************************************************
 * Copyright (c) 2008-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Steeven Lee               - import/export stuff
 *     Bastian Doetsch           - additions
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgPatchClient extends AbstractClient {

	/**
	 * Import a patch. Throws an exception if there is a conflict
	 *
	 * @see #isPatchImportConflict(HgException)
	 *
	 * @param hgRoot non null
	 * @param patchLocation non null
	 * @param options non null
	 */
	public static String importPatch(HgRoot hgRoot, File patchLocation,
			ArrayList<String> options) throws HgException {
		AbstractShellCommand command = new HgCommand("import", "Importing patch", hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.addFile(patchLocation);
		command.addOptions(options.toArray(new String[options.size()]));
		return command.executeToString();
	}

	/**
	 * Determine if the given exception indicates a conflict occurred
	 *
	 * @param e
	 *            The exception to check
	 * @return True if the exception indicates a conflict occurred
	 */
	public static boolean isPatchImportConflict(HgException e) {
		return e.getMessage().contains("patch failed to apply.");
	}

	/**
	 * @param hgRoot non null hg root
	 * @param resources non null set of files to export as diff to the latest state. If the set
	 * 	is empty, a complete diff of the hg root is exported
	 * @param patchFile non null target file for the diff
	 * @param options non null list of options, may be empty
	 * @throws HgException
	 * @return True on success
	 */
	public static boolean exportPatch(HgRoot hgRoot, List<IResource> resources,
			File patchFile, List<String> options) throws HgException {
		AbstractShellCommand command = makeExportUncommittedCommand(hgRoot, options);

		command.addFiles(resources);

		return command.executeToFile(patchFile, false);
	}

	/**
	 * export diff file to clipboard
	 *
	 * @param resources
	 * @throws HgException
	 */
	public static String exportPatch(HgRoot root, List<IResource> resources,
			List<String> options) throws HgException {
		AbstractShellCommand command = makeExportUncommittedCommand(root, options);

		command.addFiles(resources);

		return command.executeToString();
	}

	private static AbstractShellCommand makeExportUncommittedCommand(HgRoot root, List<String> options) {
		AbstractShellCommand command = new HgCommand( "diff", "Exporting patch of uncommitted changes", root, true); //$NON-NLS-1$

		command.addOptions(options.toArray(new String[options.size()]));

		return command;
	}

	/**
	 * Export a changeset to a string
	 *
	 * @param root
	 *            The repository root
	 * @param cs
	 *            The changeset
	 * @param options
	 *            Options. May be null
	 * @return The string as a patch
	 * @throws HgException
	 */
	public static String exportPatch(HgRoot root, ChangeSet cs, List<String> options)
			throws HgException {
		return makeExportPatchCommand(root, cs, options).executeToString();
	}

	/**
	 * Export a changeset to a file
	 *
	 * @param root
	 *            The repository root
	 * @param cs
	 *            The changeset
	 * @param patchFile
	 *            The file to output to
	 * @param options
	 *            Options. May be null
	 * @return True on success
	 * @throws HgException
	 */
	public static boolean exportPatch(HgRoot root, ChangeSet cs, File patchFile,
			List<String> options) throws HgException {
		return makeExportPatchCommand(root, cs, options).executeToFile(patchFile, false);
	}

	private static AbstractShellCommand makeExportPatchCommand(HgRoot root, ChangeSet cs,
			List<String> options) {
		AbstractShellCommand command = new HgCommand("export", "Exporting changeset "
				+ cs.getChangeset(), root, true);

		if (options != null) {
			command.addOptions(options.toArray(new String[options.size()]));
		}

		command.addOptions("--git");
		command.addOptions("-r", cs.getChangeset());

		return command;
	}

	/**
	 * Get a diff for a single changeSet or a range for revisions.
	 *
	 * Use the extended diff format (--git) that shows renames and file attributes.
	 *
	 * @param hgRoot The root. Must not be null.
	 * @param entry Revision of the changeset or first revision of changeset-range (if secondEntry != null). Must not be null.
	 * @param secondEntry second revision of changeset range. If null entry a diff will be created for parameter entry a a single Changeset.
	 * @return Diff as a string in extended diff format (--git).
	 * @throws HgException
	 */
	public static String getDiff(HgRoot hgRoot, MercurialRevision entry, MercurialRevision secondEntry) throws HgException {
		HgCommand diffCommand = new HgCommand("diff", //$NON-NLS-1$
				"Calculating diff between revisions", hgRoot, true);
		if( secondEntry == null ){
			diffCommand.addOptions("-c", "" + entry.getChangeSet().getRevision().getChangeset());
		} else {
			diffCommand.addOptions("-r", ""+entry.getChangeSet().getRevision().getChangeset());
			diffCommand.addOptions("-r", ""+secondEntry.getChangeSet().getRevision().getChangeset());
		}
		diffCommand.addOptions("--git");
		return diffCommand.executeToString();
	}
}
