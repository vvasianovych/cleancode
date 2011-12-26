/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgResolveClient extends AbstractClient {

	/**
	 * List merge state of files after merge
	 */
	public static List<FlaggedAdaptable> list(IResource res) throws HgException {
		AbstractShellCommand command = new HgCommand("resolve", //$NON-NLS-1$
				"Calculating conflict status for resource", res, false);
		command
				.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		command.addOptions("-l"); //$NON-NLS-1$

		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
		List<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
		if (lines.length != 1 || !"".equals(lines[0])) { //$NON-NLS-1$
			HgRoot hgRoot = getHgRoot(res);
			IProject project = res.getProject();
			for (String line : lines) {
				// Status line is always hg root relative. For those projects
				// which has different project root hg root relative path must
				// be converted to project relative
				IResource iFile = ResourceUtils.convertRepoRelPath(hgRoot, project, line.substring(2));
				if(iFile != null){
					FlaggedAdaptable fa = new FlaggedAdaptable(iFile, line.charAt(0));
					result.add(fa);
				}
			}
		}
		return result;
	}

	/**
	 * List merge state of files after merge
	 */
	public static List<FlaggedAdaptable> list(HgRoot hgRoot) throws HgException {
		AbstractShellCommand command = new HgCommand("resolve", "Listing conflict status", hgRoot, false);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		command.addOptions("-l"); //$NON-NLS-1$
		String[] lines = command.executeToString().split("\n"); //$NON-NLS-1$
		List<FlaggedAdaptable> result = new ArrayList<FlaggedAdaptable>();
		if (lines.length != 1 || !"".equals(lines[0])) { //$NON-NLS-1$
			for (String line : lines) {
				// Status line is always hg root relative. For those projects
				// which has different project root (always deeper than hg root)
				// hg root relative path must be converted
				String repoRelPath = line.substring(2);
				IResource iFile = ResourceUtils.getFileHandle(hgRoot.toAbsolute(new Path(repoRelPath)));
				if(iFile != null){
					FlaggedAdaptable fa = new FlaggedAdaptable(iFile, line.charAt(0));
					result.add(fa);
				}
			}
		}
		return result;
	}

	/**
	 * Mark a resource as resolved ("R")
	 */
	public static String markResolved(IFile ifile) throws HgException {
		File file = ResourceUtils.getFileHandle(ifile);
		HgCommand command = new HgCommand("resolve", //$NON-NLS-1$
				"Marking resource as resolved", ifile, false);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(command
				.getHgRoot()));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
			try {
			command.addOptions("-m", file.getCanonicalPath()); //$NON-NLS-1$
			String result = command.executeToString();
			// cleanup .orig files left after merge
			File origFile = new File(file.getAbsolutePath() + ".orig");
			if(origFile.isFile()){
				IResource fileToDelete = ResourceUtils.convert(origFile);
				boolean deleted = origFile.delete();
				if(!deleted){
					MercurialEclipsePlugin.logInfo("Failed to cleanup " + origFile + " file after merge", null);
				} else {
					try {
						fileToDelete.refreshLocal(IResource.DEPTH_ZERO, null);
					} catch (CoreException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
			}
			refreshStatus(ifile);
			return result;
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Mark a resource as unresolved ("U")
	 */
	public static String markUnresolved(IFile ifile) throws HgException {
		IPath path = ResourceUtils.getPath(ifile);
		if(path.isEmpty()) {
			throw new HgException("Failed to unresolve: location is unknown: " + ifile);
		}
		File file = path.toFile();
		HgCommand command = new HgCommand("resolve", //$NON-NLS-1$
				"Marking resource as unresolved", ifile, false);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(command
				.getHgRoot()));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.IMERGE_TIMEOUT);
		try {
			command.addOptions("-u", file.getCanonicalPath()); //$NON-NLS-1$
			String result = command.executeToString();
			refreshStatus(ifile);
			return result;
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	private static void refreshStatus(IResource res) throws HgException {
		MercurialStatusCache.getInstance().refreshStatus(res, null);
		ResourceUtils.touch(res);
	}

	/**
	 * Executes resolve command to find change sets necessary for merging
	 * <p>
	 * WARNING: This method potentially reverts changes!
	 * <p>
	 * Future: We should write some python to interface with the Mercurial API directly to get this
	 * info so we don't have to do operations with side effects or rely on --debug output.
	 *
	 * @param file
	 *            The file to consider
	 * @return An array of length 3 of changeset ids: result[0] - 'my' result[1] - 'other' result[2]
	 *         - 'base'
	 * @throws HgException
	 */
	public static String[] restartMergeAndGetChangeSetsForCompare(IFile file) throws HgException {
		String[] results = new String[3];
		HgCommand command = new HgCommand("resolve", "Invoking resolve to find parent information", file, false);

		command.addOptions("--config", "ui.merge=internal:mustfail", "--debug");
		command.addFiles(file);

		String stringResult = "";
		try {
			command.executeToString();
		} catch (HgException e) {
			// exception is expected here
			stringResult = e.getMessage();
		}

		String filename = file.getName();
		String patternString = "my .*" + filename + "@?([0-9a-fA-F]*)\\+?[\\s]"
			+ "other .*" + filename + "@?([0-9a-fA-F]*)\\+?[\\s]"
			+ "ancestor .*" + filename + "@?([0-9a-fA-F]*)\\+?[\\s]";

		Matcher matcher = Pattern.compile(patternString).matcher(stringResult);

		if (matcher.find() && matcher.groupCount() == 3) {
			results[0] = matcher.group(1);	// my
			results[1] = matcher.group(2);	// other
			results[2] = matcher.group(3);	// ancestor
		}

		return results;
	}

}
