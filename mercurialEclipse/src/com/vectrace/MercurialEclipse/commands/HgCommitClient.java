/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch
 *     StefanC
 *     Zsolt Koppany (Intland)
 *     Adam Berkes (Intland)
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgCommitClient extends AbstractClient {

	/**
	 * Commit given resources and refresh the caches for the associated projects
	 */
	public static String commitResources(List<IResource> resources, String user, String message, IProgressMonitor monitor, boolean closeBranch) throws HgException {
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);

		StringBuilder commit = new StringBuilder();
		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot root = mapEntry.getKey();
			if (monitor != null) {
				if (monitor.isCanceled()) {
					break;
				}
				monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
			}
			List<IResource> files = mapEntry.getValue();
			commit.append(commit(root, AbstractClient.toFiles(files), user, message, closeBranch));
		}
		for (HgRoot root : resourcesByRoot.keySet()) {
			new RefreshRootJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		}
		return commit.toString();
	}

	/**
	 * Commit given hg root with all checked out/added/deleted changes and refresh the caches for the assotiated projects
	 */
	public static String commitResources(HgRoot root, boolean closeBranch, String user, String message, IProgressMonitor monitor) throws HgException {
		monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
		List<File> emptyList = Collections.emptyList();
		String commit = commit(root, emptyList, user, message, closeBranch);
		new RefreshRootJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		return commit;
	}

	/**
	 * Performs commit. No refresh of any cashes is done afterwards.
	 *
	 * <b>Note</b> clients should not use this method directly, it is NOT private
	 *  for tests only
	 */
	protected static String commit(HgRoot hgRoot, List<File> files, String user, String message,
			boolean closeBranch) throws HgException {
		HgCommand command = new HgCommand("commit", "Committing resources", hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
		command.addUserName(user);
		if (closeBranch) {
			command.addOptions("--close-branch");
		}
		File messageFile = addMessage(command, message);
		try {
			command.addFiles(files);
			String result = command.executeToString();
			command.rememberUserName();
			return result;
		} finally {
			deleteMessage(messageFile);
		}
	}

	/**
	 * Add the message to the command. If possible a file is created to do this (assumes the command
	 * accepts the -l parameter)
	 *
	 * @return The file that must be deleted
	 * @see #deleteMessage(File)
	 */
	public static File addMessage(HgCommand command, String message) {
		File messageFile = saveMessage(message, command);

		if (messageFile != null && messageFile.isFile()) {
			command.addOptions("-l", messageFile.getAbsolutePath());
			return messageFile;
		}

		// fallback in case of unavailable message file
		message = quote(message.trim());
		if (message.length() != 0) {
			command.addOptions("-m", message);
		} else {
			command.addOptions("-m",
					com.vectrace.MercurialEclipse.dialogs.Messages
							.getString("CommitDialog.defaultCommitMessage"));
		}

		return messageFile;
	}

	/**
	 * Commit given project after the merge and refresh the caches.
	 * Implementation note: after merge, no files should be specified.
	 */
	public static String commit(HgRoot hgRoot, String user, String message) throws HgException {
		HgCommand command = new HgCommand("commit", "Committing all changes", hgRoot, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
		command.addUserName(user);
		File messageFile = addMessage(command, message);
		try {
			MercurialUtilities.setMergeViewDialogShown(false);
			String result = command.executeToString();
			command.rememberUserName();
			new RefreshRootJob(hgRoot, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
			return result;
		} finally {
			deleteMessage(messageFile);
		}
	}

	private static String quote(String str) {
		if (str != null) {
			str = str.trim();
		}
		if (str == null || str.length() == 0 || !MercurialUtilities.isWindows()) {
			return str;
		}
		// escape quotes, otherwise commit will fail at least on windows
		return str.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static File saveMessage(String message, HgCommand command) {
		Writer writer = null;
		try {
			File messageFile = File.createTempFile("hgcommitmsg", ".txt");
			writer = new OutputStreamWriter(new FileOutputStream(messageFile),
					command.getHgRoot().getEncoding());
			writer.write(message.trim());
			return messageFile;
		} catch (IOException ex) {
			MercurialEclipsePlugin.logError(ex);
			return null;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ex) {
					MercurialEclipsePlugin.logError(ex);
				}
			}
		}
	}

	public static void deleteMessage(File messageFile) {
		// Try to delete normally, and if not successful
		// leave it for the JVM exit - I use it in case
		// mercurial accidentally locks the file.
		if (messageFile != null && !messageFile.delete()) {
			messageFile.deleteOnExit();
		}
	}
}
