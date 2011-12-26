/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - initial
 *     Bastian Doetsch           - changes
 *     Brian Wallis              - getMergeStatus
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)	 - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class HgStatusClient extends AbstractClient {

	// expected output for merge:
	// b63617c1e3460bd87eb51d2b8841b37fff1834d6+00838f86e1024072e715d31f477262d5162acd09+ default
	// match second part (the one we merge with)
	// output for "usual" state:
	// b63617c1e3460bd87eb51d2b8841b37fff1834d6+ default
	// OR b63617c1e3460bd87eb51d2b8841b37fff1834d6 hallo branch
	// + after the id is the "dirty" flag - if some files are not committed yet
	//
	// As well in Mercurial 1.6.0 during some rebases the following output:
	//    filtering src/nexj/core/admin/platform/websphere/WebSphereInstaller.java through
	//    filtering src/nexj/core/meta/j2ee/ibmconfig/cells/defaultCell/applications/defaultApp/deployments/defaultApp/deployment.xml through
	//    filtering src/nexj/core/meta/sys/system.chtypes through
	//    02bfc05967b86ba65a0cb990178638e4c491c865+d35923c18f8f564c6205e722119d88c6daa3f56d+ default
	// These leading lines are ignored.
	private static final Pattern ID_MERGE_AND_BRANCH_IGNORE_PATTERN = Pattern.compile("^filtering\\s.+\\sthrough\\s*$");

	//             group 1                         group 2                             group 3
	// (first parent, optional dirty flag)(merge parent, optional dirty flag) space (branch name)
	private static final Pattern ID_MERGE_AND_BRANCH_PATTERN = Pattern.compile("^([0-9a-z]+\\+?)([0-9a-z]+)?\\+?\\s+(.+)$");

	public static String getStatusWithoutIgnored(HgRoot root, IResource res) throws HgException {
		AbstractShellCommand command = new HgCommand("status", "Calculating resource status", root, true); //$NON-NLS-1$

		// modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduc"); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		if (res.getType() == IResource.FILE) {
			command.addFiles(res);
		}
		return command.executeToString();
	}

	public static String getStatusWithoutIgnored(HgRoot root) throws HgException {
		AbstractShellCommand command = new HgCommand("status", "Calculating resource status", root, true); //$NON-NLS-1$

		// modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduc"); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		return command.executeToString();
	}

	/**
	 * @param hgRoot non null
	 * @return non null, but probably empty array with root relative file paths with all
	 * files under the given root, which are untracked by hg
	 */
	public static String[] getUntrackedFiles(HgRoot hgRoot) throws HgException {
		AbstractShellCommand command = new HgCommand("status", "Calculating untracked files", hgRoot, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-u", "-n"); //$NON-NLS-1$ //$NON-NLS-2$
		return command.executeToString().split("\n"); //$NON-NLS-1$
	}

	public static boolean isDirty(HgRoot root) throws HgException {
		AbstractShellCommand command = new HgCommand("status", "Calculating dirty state", root, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-mard"); // modified, added, removed, deleted //$NON-NLS-1$
		return command.executeToBytes().length != 0;
	}

	/**
	 * @param root non null
	 * @return current changeset id, merge id and branch name for the working directory
	 * @throws HgException
	 */
	public static String[] getIdMergeAndBranch(HgRoot root) throws HgException {
		AbstractShellCommand command = new HgCommand("id", "Identifying status", root, true); //$NON-NLS-1$
		// Full global IDs + branch name
		command.addOptions("-ib", "--debug"); //$NON-NLS-1$ //$NON-NLS-2$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		String versionIds = null;
		BufferedReader br = new BufferedReader(new StringReader(command.executeToString().trim()));

		try {
			while ((versionIds = br.readLine()) != null) {
				if (!ID_MERGE_AND_BRANCH_IGNORE_PATTERN.matcher(versionIds).matches()) {
					break;
				}
			}
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
		} finally {
			try {
				br.close();
			} catch (IOException ex) {
			}
		}

		Matcher m = ID_MERGE_AND_BRANCH_PATTERN.matcher((versionIds == null) ? "" : versionIds);
		String mergeId = null;
		String branch = Branch.DEFAULT;
		// current working directory id
		String id = "";
		if (m.matches() && m.groupCount() > 2) {
			id = m.group(1);
			mergeId = m.group(2);
			branch = m.group(3);
		}
		if (id.endsWith("+")) {
			id = id.substring(0, id.length() - 1);
		}
		return new String[] { id, mergeId, branch };
	}

	public static String getStatusWithoutIgnored(HgRoot root, List<IResource> files) throws HgException {
		AbstractShellCommand command = new HgCommand("status", //$NON-NLS-1$
				"Calculating status for " + files.size() + " resources", root, true);

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		// modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduc"); //$NON-NLS-1$
		command.addFiles(files);
		return command.executeToString();
	}

	/**
	 * @return root relative paths of changed files, never null
	 */
	public static String[] getDirtyFiles(HgRoot root) throws HgException {
		AbstractShellCommand command = new HgCommand("status", "Calculating dirty resources", root, true); //$NON-NLS-1$

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-mard"); //$NON-NLS-1$
		String result = command.executeToString();
		if (result == null || result.length() == 0) {
			return new String[0];
		}
		// now we will have status AND path info
		String[] status = result.split("\n");
		for (int i = 0; i < status.length; i++) {
			// remove status info from status AND path line
			status[i] = status[i].substring(2);
		}
		return status;
	}

	public static Set<IPath> getDirtyFilePaths(HgRoot root) throws HgException {
		String[] dirtyFiles = getDirtyFiles(root);
		IPath rootPath = new Path(root.getAbsolutePath());
		Set<IPath> resources = new HashSet<IPath>();
		for (String rootRelativePath : dirtyFiles) {
			// determine absolute path
			IPath path = rootPath.append(rootRelativePath);
			resources.add(path);
		}
		return resources;
	}

	/**
	 * Returns <b>possible</b> ancestor of the given file, if given file is a result of
	 * a copy or rename operation.
	 * <p>
	 * <b>Note</b>: this is a very inefficient algorithm, which may need a lot of time to
	 * complete.
	 *
	 * @param file
	 *            successor path (as full absolute file path)
	 * @param root
	 *            hg root
	 * @param firstKnownRevision the version at which the information about possible
	 *  parent file should be retrieved
	 *
	 * @return full absolute file path which was the source of the given file one changeset
	 *         before given version, or null if the given file was not copied or renamed
	 *         at given version.
	 * @throws HgException
	 */
	public static File getPossibleSourcePath(HgRoot root, File file, int firstKnownRevision) throws HgException{
		return getPossibleSourcePath(root, file, firstKnownRevision - 1, "" + firstKnownRevision);
	}

	/**
	 * <b>Guesses</b> a <b>possible</b> ancestor of the given file, if given file is a result of
	 * a copy or rename operation.
	 * <p>
	 * <b>Note</b>: this is a fast but inaccurate algorithm, which may not return expected
	 * information
	 *
	 * @param file
	 *            successor path (as full absolute file path)
	 * @param root
	 *            hg root
	 * @param firstKnownRevision the version at which the information about possible
	 *  parent file should be retrieved
	 * @return full absolute file path which was the source of the given file one changeset
	 *         before given version, or null if the given file was not copied or renamed
	 *         at given version.
	 * @throws HgException
	 */
	public static File guessPossibleSourcePath(HgRoot root, File file, int firstKnownRevision) throws HgException {
		// See issue #10302: we trying to cheat: "hg status -arC --rev firstKnownRevision:tip file" will return
		// A file
		//   source_file
		// in case the given file was known between "firstKnownRevision" and "tip"
		// TODO not sure if "tip" works always as expected, but now it's my best guess for the upper limit
		return getPossibleSourcePath(root, file, firstKnownRevision, HgRevision.TIP.getChangeset());
	}

	private static File getPossibleSourcePath(HgRoot root, File file, int firstRev, String secondRev) throws HgException{
		AbstractShellCommand command = new HgCommand("status", "Finding resource status", root, true); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		command.addOptions("-arC"); //$NON-NLS-1$
		command.addOptions("--rev"); //$NON-NLS-1$
		command.addOptions(firstRev + ":" + secondRev); //$NON-NLS-1$
		command.addFile(file);
		String result = command.executeToString();
		if (result == null || result.length() == 0) {
			return null;
		}

		String relativePath = root.toRelative(file);

		String[] statusAndFileNames = result.split("\n"); //$NON-NLS-1$
		String prefixAdded = MercurialStatusCache.CHAR_ADDED + " ";
		String prefixRemoved = MercurialStatusCache.CHAR_REMOVED + " ";
		for (int i = 0; i < statusAndFileNames.length; i++) {
			// looks like: "A folder\foo" or "R folder\foo" or "folder\foo"
			String pathWithStatus = statusAndFileNames[i];
			if(pathWithStatus.startsWith(prefixAdded) || pathWithStatus.startsWith(prefixRemoved)){
				if(i + 1 < statusAndFileNames.length && pathWithStatus.endsWith(relativePath)){
					// XXX should not just trim whitespace in the path, if it contains whitespaces, it will not work
					// on the other side it's just idiotic to have filenames with leading or trailing spaces
					String nextLine = statusAndFileNames[i + 1].trim();
					if(!nextLine.startsWith(prefixAdded) && !nextLine.startsWith(prefixRemoved)) {
						return new File(root, nextLine);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Execute 'hg status --rev cs --rev csparent', with bundle overlay if applicable
	 *
	 * @param cs
	 *            The changeset to use
	 * @return Status output
	 * @throws HgException
	 */
	public static String getStatusForChangeset(ChangeSet cs) throws HgException {
		HgCommand command = new HgCommand(
				"status", //$NON-NLS-1$
				"Calcuting resources changes in changeset " + cs.getChangeset(), cs.getHgRoot(),
				true);

		if (cs.getBundleFile() != null) {
			try {
				command.setBundleOverlay(cs.getBundleFile());
			} catch (IOException e) {
				throw new HgException("Couldn't set bundle overlay", e);
			}
		}

		// modified, added, removed, deleted
		command.addOptions("-mard"); //$NON-NLS-1$
		command.addOptions("--rev", cs.getParentRevision(0).getChangeset());
		command.addOptions("--rev", cs.getRevision().getChangeset());

		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);

		return command.executeToString();
	}
}
