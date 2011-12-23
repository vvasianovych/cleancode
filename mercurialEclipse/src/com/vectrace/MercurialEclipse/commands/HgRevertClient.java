/*******************************************************************************
 * Copyright (c) 2005-2010 Andrei Loskutov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov          - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.Messages;

/**
 * @author Andrei
 */
public class HgRevertClient extends AbstractClient {

	/**
	 * @param monitor non null
	 * @param hgRoot the root of all given resources
	 * @param resources resources to revert
	 * @param cs might be null
	 * @return a copy of file paths affected by this command, if any. Never returns null,
	 * but may return empty list. The elements of the set are absolute file paths.
	 * @throws HgException
	 */
	public static Set<String> performRevert(IProgressMonitor monitor, HgRoot hgRoot,
			List<IResource> resources, ChangeSet cs) throws HgException {
		Set<String> fileSet = new HashSet<String>();
		monitor.subTask(Messages.getString("ActionRevert.reverting") + " " + hgRoot.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		// if there are too many resources, do several calls
		int size = resources.size();
		if(size == 0) {
			MercurialUtilities.setMergeViewDialogShown(false);
			return fileSet;
		}
		IResource firstFile = resources.get(0);
		if(size == 1 && cs != null && (cs.isMoved(firstFile) || cs.isRemoved(firstFile))) {
				HgRevision parentRevision = cs.getParentRevision(0, true);
				HgCommand command = createRevertCommand(hgRoot, "Reverting " + firstFile.getName());
				command.addOptions("--rev", parentRevision.getChangeset());
				command.addFiles(firstFile);
				if(cs.isMoved(firstFile)) {
					FileStatus status = cs.getStatus(firstFile);
					if(status != null) {
						IPath path = status.getAbsoluteCopySourcePath();
						command.addFile(path.toFile());
					}
				}
				command.executeToString();
				fileSet.addAll(command.getAffectedFiles());
		} else {
			// if there are too many resources, do several calls
			// From 1.8 hg can do it in one call
			if(!HgFeatures.LISTFILE.isEnabled()) {
				int delta = AbstractShellCommand.MAX_PARAMS - 1;
				for (int i = 0; i < size && !monitor.isCanceled(); i += delta) {
					// the last argument will be replaced with a path
					HgCommand command = createRevertCommand(hgRoot, "Reverting resource " + i + " of " + size);
					if (cs != null) {
						command.addOptions("--rev", cs.getChangeset());
					}
					command.addFiles(resources.subList(i, Math.min(i + delta, size)));
					command.executeToString();
					fileSet.addAll(command.getAffectedFiles());
				}
			} else {
				// the last argument will be replaced with a path
				HgCommand command = createRevertCommand(hgRoot, "Reverting " + size + " resources");
				if (cs != null) {
					command.addOptions("--rev", cs.getChangeset());
				}
				command.addFiles(resources);
				command.executeToString();
				fileSet.addAll(command.getAffectedFiles());
			}
		}
		monitor.worked(1);

		MercurialUtilities.setMergeViewDialogShown(false);
		return fileSet;
	}

	public static void performRevertAll(IProgressMonitor monitor, HgRoot hgRoot) throws HgException {
		monitor.subTask(Messages.getString("ActionRevert.reverting") + " " + hgRoot.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$

		HgCommand command = createRevertCommand(hgRoot, "Reverting all resources");
		command.addOptions("--all");
		command.executeToString();

		MercurialUtilities.setMergeViewDialogShown(false);
	}

	private static HgCommand createRevertCommand(HgRoot hgRoot, String message) {
		HgCommand command = new HgCommand("revert", message, hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
		command.addOptions("--no-backup");
		return command;
	}
}
