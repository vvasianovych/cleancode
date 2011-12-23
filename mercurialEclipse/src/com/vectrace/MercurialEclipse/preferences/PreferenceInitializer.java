/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Jérôme Nègre              - adding label decorator section
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (intland)   - bug fixes
 *     Philip Graf               - use default timeout from preferences
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.synchronize.PresentationMode;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	private static final boolean IS_WINDOWS = MercurialUtilities.isWindows();

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();

		// per default, we use exact the executable we have (if any) on board
		store.setDefault(USE_BUILT_IN_HG_EXECUTABLE, true);

		// try to find out, IF we have the built-in hg executable
		detectAndSetHgExecutable(store);

		store.setDefault(PREF_AUTO_SHARE_PROJECTS, true);

		store.setDefault(PREF_SYNC_ALL_PROJECTS_IN_REPO, true);
		store.setDefault(PREF_SYNC_ONLY_CURRENT_BRANCH, true);
		store.setDefault(PREF_SYNC_PRESENTATION_MODE, PresentationMode.COMPRESSED_TREE.name());

		// currently this reduces performance 2x => so disable per default
		store.setDefault(PREF_ENABLE_SUBREPO_SUPPORT, false);

		// "Highest" importance should be default, like "merge conflict"
		// when having 2 different statuses in a folder it should have the more important one
		store.setDefault(LABELDECORATOR_LOGIC, LABELDECORATOR_LOGIC_HB);

		store.setDefault(RESOURCE_DECORATOR_SHOW_CHANGESET, false);
		store.setDefault(RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET, false);

		store.setDefault(SYNC_COMPUTE_FULL_REMOTE_FILE_STATUS, true);

		store.setDefault(LOG_BATCH_SIZE, 500);
		store.setDefault(STATUS_BATCH_SIZE, 10);
		store.setDefault(COMMIT_MESSAGE_BATCH_SIZE, 10);
		store.setDefault(ENABLE_FULL_GLOG, true);

		// blue
		store.setDefault(PREF_CONSOLE_COMMAND_COLOR, "0,0,255");
		// black
		store.setDefault(PREF_CONSOLE_MESSAGE_COLOR, "0,0,0");
		// red
		store.setDefault(PREF_CONSOLE_ERROR_COLOR, "255,0,0");

		store.setDefault(PREF_CONSOLE_SHOW_ON_STARTUP, false);
		store.setDefault(PREF_CONSOLE_LIMIT_OUTPUT, true);
		store.setDefault(PREF_CONSOLE_HIGH_WATER_MARK, 100000);

		store.setDefault(PREF_DECORATE_WITH_COLORS, true);
		store.setDefault(PREF_SHOW_COMMENTS, true);
		store.setDefault(PREF_SHOW_PATHS, true);
		// See issue #13662: do not show diffs per default: they may cause OOM on huge changesets
		store.setDefault(PREF_SHOW_DIFFS, false);
		store.setDefault(PREF_SHOW_ALL_TAGS, false);
		store.setDefault(PREF_SHOW_GOTO_TEXT, true);
		store.setDefault(PREF_AFFECTED_PATHS_LAYOUT, LAYOUT_HORIZONTAL);
		store.setDefault(PREF_SIGCHECK_IN_HISTORY, false);

		store.setDefault(PREF_HISTORY_MERGE_CHANGESET_BACKGROUND, "255,210,210");
		store.setDefault(PREF_HISTORY_MERGE_CHANGESET_FOREGROUND, "0,0,0");


		int defaultTimeout = TimeoutPreferencePage.DEFAULT_TIMEOUT;
		store.setDefault(DEFAULT_TIMEOUT, defaultTimeout);

		// remote operations are always longer than local
		store.setDefault(CLONE_TIMEOUT, defaultTimeout * 10);
		store.setDefault(PUSH_TIMEOUT, defaultTimeout * 10);
		store.setDefault(PULL_TIMEOUT, defaultTimeout * 10);

		store.setDefault(UPDATE_TIMEOUT, defaultTimeout);
		store.setDefault(COMMIT_TIMEOUT, defaultTimeout);
		store.setDefault(IMERGE_TIMEOUT, defaultTimeout);
		store.setDefault(LOG_TIMEOUT, defaultTimeout);
		store.setDefault(STATUS_TIMEOUT, defaultTimeout);
		store.setDefault(ADD_TIMEOUT, defaultTimeout);
		store.setDefault(REMOVE_TIMEOUT, defaultTimeout);

		String defaultUsername = store.getDefaultString(MERCURIAL_USERNAME);
		if(defaultUsername == null || defaultUsername.length() == 0){
			// the task below may block UI thread and cause entire system to wait forever
			// therefore start job execution, with the hope, that the user name is not needed
			// immediately after startup (usualy it is required by commit/tag/merge etc).
			Job job = new Job("Detecting hg user name"){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					store.setDefault(MERCURIAL_USERNAME, MercurialUtilities.getDefaultUserName());
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.INTERACTIVE);
			job.setSystem(true);
			job.schedule(200);
		}
		store.setDefault(PREF_USE_MERCURIAL_USERNAME, false);
		store.setDefault(PREF_DEFAULT_REBASE_KEEP_BRANCHES, false);
		store.setDefault(PREF_USE_EXTERNAL_MERGE, false);
		store.setDefault(PREF_DEFAULT_TRANSPLANT_FROM_LOCAL_BRANCHES, false);
		store.setDefault(PREF_CLONE_UNCOMPRESSED, false);

		store.setDefault(PREF_PRESELECT_UNTRACKED_IN_COMMIT_DIALOG, false);
		store.setDefault(PREF_VERIFY_SERVER_CERTIFICATE, true);

		store.setDefault(PREF_SHOW_PULL_WARNING_DIALOG, MessageDialogWithToggle.PROMPT);
		store.setDefault(PREF_SHOW_MULTIPLE_PROJECTS_DIALOG, MessageDialogWithToggle.PROMPT);
	}

	private static File checkForPossibleHgExecutables() {
		File hgExecutable = null;

		String envPath = System.getenv("PATH");

		if (envPath != null) {
			String pathSeparator = String.valueOf(File.pathSeparatorChar);
			String execSuffix = IS_WINDOWS ? ".exe" : "";
			for (StringTokenizer st = new StringTokenizer(envPath, pathSeparator, false); st.hasMoreElements(); ) {
				String execPath = st.nextToken() + "/hg" + execSuffix;
				File file = new File(execPath);
				if (file.isFile()) {
					hgExecutable = file;
					break;
				}
			}
		}

		if (hgExecutable == null && !IS_WINDOWS) {
			String extraPath[] = {
				"/usr/bin/hg",
				"/usr/local/bin/hg",	// default on MacOS
				"/opt/local/bin/hg",	// if installed via MacPorts
			};

			for (String fileName : extraPath) {
				File file = new File(fileName);
				if (file.isFile()) {
					hgExecutable = file;
					break;
				}
			}
		}
		return hgExecutable;
	}

	private static void detectAndSetHgExecutable(IPreferenceStore store) {
		// Currently only tested on Windows. The binary is expected to be found
		// at "os\win32\x86\hg.exe" (relative to the plugin/fragment directory)
		File hgExecutable = getIntegratedHgExecutable();
		String defaultExecPath;
		String existingValue = store.getString(MERCURIAL_EXECUTABLE);
		if (hgExecutable == null) {
			hgExecutable = checkForPossibleHgExecutables();
		}
		if (hgExecutable == null) {
			defaultExecPath = "hg";
			if(existingValue != null && !new File(existingValue).isFile()){
				store.setValue(MERCURIAL_EXECUTABLE, defaultExecPath);
			}
		} else {
			defaultExecPath = hgExecutable.getPath();
			if (store.getBoolean(USE_BUILT_IN_HG_EXECUTABLE)
					|| (existingValue == null || !new File(existingValue).isFile())) {
				store.setValue(MERCURIAL_EXECUTABLE, defaultExecPath);
			}
		}
		store.setDefault(MERCURIAL_EXECUTABLE, defaultExecPath);
	}

	/**
	 * @return an full absolute path to the embedded hg executable from the (fragment)
	 * plugin. This path is guaranteed to point to an <b>existing</b> file. Returns null
	 * if the file cannot be found, does not exists or is not a file at all.
	 */
	public static File getIntegratedHgExecutable(){
		IPath path = IS_WINDOWS ? new Path("$os$/hg.exe") : new Path("$os$/hg");
		URL url = FileLocator.find(MercurialEclipsePlugin.getDefault().getBundle(), path, null);
		if(url == null) {
			return null;
		}
		try {
			url = FileLocator.toFileURL(url);
			File execFile = new File(url.getPath());
			if (execFile.isFile()) {
				return execFile.getAbsoluteFile();
			}
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return null;
	}
}
