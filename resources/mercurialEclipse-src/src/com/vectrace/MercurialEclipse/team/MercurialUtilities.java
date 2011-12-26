/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - some updates
 *     Stefan Groschupf          - logError
 *     Bastian Doetsch           - updates, cleanup and documentation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgConfigClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.utils.IniFile;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * Class that offers Utility methods for working with the plug-in.
 *
 * @author zingo
 *
 */
public final class MercurialUtilities {
	private static final boolean IS_WINDOWS = File.separatorChar == '\\';
	private static final Map<RGB, Color> COLOR_MAP = new HashMap<RGB, Color>();
	private static final Map<FontData, Font> FONT_MAP = new HashMap<FontData, Font>();

	/**
	 * This class is full of utilities metods, useful allover the place
	 */
	private MercurialUtilities() {
		// don't call me
	}

	public static boolean isWindows() {
		return IS_WINDOWS;
	}

	/**
	 * Determines if the configured Mercurial executable can be called.
	 *
	 * @return true if no error occurred while calling the executable, false otherwise
	 */
	public static boolean isHgExecutableCallable() {
		try {
			Runtime.getRuntime().exec(getHGExecutable());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns the hg executable stored in the plug-in preferences. If it's not defined, "hg" is
	 * returned as default.
	 *
	 * @return the path to the executable or, if not defined "hg"
	 * @deprecated Use {@link com.vectrace.MercurialEclipse.commands.HgClients#getExecutable()}
	 */
	@Deprecated
	public static String getHGExecutable() {
		return HgClients.getPreference(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, "hg"); //$NON-NLS-1$
	}

	/**
	 * Fetches a preference from the plug-in's preference store. If no preference could be found in
	 * the store, the given default is returned.
	 *
	 * @param preferenceConstant
	 *            the string identifier for the constant.
	 * @param defaultIfNotSet
	 *            the default to return if no preference was found.
	 * @return the preference or the default
	 */
	public static String getPreference(String preferenceConstant, String defaultIfNotSet) {
		IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		// This returns "" if not defined
		String pref = preferenceStore.getString(preferenceConstant);

		if (pref.length() > 0) {
			return pref;
		}
		return defaultIfNotSet;
	}

	/**
	 * Gets the configured executable if it's callable (@see
	 * {@link MercurialUtilities#isHgExecutableCallable()}.
	 *
	 * If configureIfMissing is set to true, the configuration will be started and afterwards the
	 * executable stored in the preferences will be checked if it is callable. If true, it is
	 * returned, else "hg" will be returned. If the parameter is set to false, it will returns "hg"
	 * if no preference is set.
	 *
	 * @param configureIfMissing
	 *            flag if configuration should be started if hg is not callable.
	 * @return the hg executable path
	 */
	public static String getHGExecutable(boolean configureIfMissing) {
		if (isHgExecutableCallable()) {
			return getHGExecutable();
		}
		if (configureIfMissing) {
			configureHgExecutable();
			return getHGExecutable();
		}
		return "hg"; //$NON-NLS-1$
	}

	/**
	 * Checks the GPG Executable is callable and returns it if it is.
	 *
	 * Otherwise, if configureIfMissing is set to true, configuration will be started and the new
	 * command is tested for callability. If there's no preference found after configuration, "gpg"
	 * will be returned as default.
	 *
	 * @param configureIfMissing
	 *            flag, if configuration should be started if gpg is not callable.
	 * @return the gpg executable path
	 */
	public static String getGpgExecutable(boolean configureIfMissing) {
		if (isGpgExecutableCallable()) {
			return getGpgExecutable();
		}
		if (configureIfMissing) {
			configureGpgExecutable();
			return getGpgExecutable();
		}
		return "gpg"; //$NON-NLS-1$
	}

	/**
	 * Starts configuration for Gpg executable by opening the preference page.
	 */
	public static void configureGpgExecutable() {
		configureHgExecutable();
	}

	private static boolean isGpgExecutableCallable() {
		try {
			Runtime.getRuntime().exec(getGpgExecutable());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns the executable for gpg. If it's not defined, false is returned
	 *
	 * @return gpg executable path or "gpg", if it's not set.
	 */
	public static String getGpgExecutable() {
		String executable = HgClients.getPreference(MercurialPreferenceConstants.GPG_EXECUTABLE,
				"gpg"); //$NON-NLS-1$
		if (executable == null || executable.length() == 0) {
			return "false"; //$NON-NLS-1$
		}
		return executable;
	}

	/**
	 * Starts the configuration for Mercurial executable by opening the preference page.
	 */
	public static void configureHgExecutable() {
		final String jobName = Messages
				.getString("MercurialUtilities.openingPreferencesForConfiguringMercurialEclipse");
		SafeUiJob job = new SafeUiJob(jobName) {

			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				String pageId = "com.vectrace.MercurialEclipse.prefspage"; //$NON-NLS-1$
				PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(getDisplay()
						.getActiveShell(), pageId, null, null);
				dlg.setErrorMessage(Messages
						.getString("MercurialUtilities.errorNotConfiguredCorrectly") //$NON-NLS-1$
						+ Messages.getString("MercurialUtilities.runDebugInstall")); //$NON-NLS-1$
				dlg.open();
				return super.runSafe(monitor);
			}

			@Override
			public boolean belongsTo(Object family) {
				return jobName.equals(family);
			}
		};
		IJobManager jobManager = Job.getJobManager();
		jobManager.cancel(jobName);
		Job[] jobs = jobManager.find(jobName);
		if (jobs.length == 0) {
			job.schedule(50);
		}
	}

	/**
	 * Returns the username for hg as configured in preferences. If it's not defined in the
	 * preference store, null is returned.
	 * <p>
	 * <b>Note:</b> Preferred way to access user commit name is to use
	 * {@link HgCommitMessageManager#getDefaultCommitName(HgRoot)}
	 *
	 * @return hg username or empty string, never null
	 */
	public static String getDefaultUserName() {
		IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		// This returns "" if not defined
		String username = preferenceStore
				.getString(MercurialPreferenceConstants.MERCURIAL_USERNAME);

		// try to read username via hg showconfig
		if (StringUtils.isEmpty(username)) {
			try {
				username = HgConfigClient.getHgConfigLine("ui.username");
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		// try to read mercurial hgrc in default locations
		String home = System.getProperty("user.home");
		if (StringUtils.isEmpty(username)) {
			username = readUsernameFromIni(home + "/.hgrc");
		}

		if (isWindows()) {
			if (StringUtils.isEmpty(username)) {
				username = readUsernameFromIni(home + "/Mercurial.ini");
			}

			if (StringUtils.isEmpty(username)) {
				username = readUsernameFromIni("C:/Mercurial/Mercurial.ini");
			}
		}

		if (StringUtils.isEmpty(username)) {
			// use system username
			username = System.getProperty("user.name");
		}

		// never return null!
		if (username == null) {
			username = "";
		}
		return username;
	}

	private static String readUsernameFromIni(String filename) {
		String username;
		try {
			IniFile iniFile = new IniFile(filename);
			username = iniFile.getKeyValue("ui", "username");
		} catch (FileNotFoundException e) {
			username = null;
		}
		return username;
	}

	public static boolean isCommandAvailable(String command, QualifiedName sessionPropertyName,
			String extensionEnabler) throws HgException {
		try {
			boolean returnValue;
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			Object prop = workspaceRoot.getSessionProperty(sessionPropertyName);
			if (prop != null) {
				returnValue = ((Boolean) prop).booleanValue();
			} else {
				returnValue = AbstractClient.isCommandAvailable(command, extensionEnabler);
				workspaceRoot.setSessionProperty(sessionPropertyName, Boolean.valueOf(returnValue));
			}
			return returnValue;
		} catch (CoreException e) {
			throw new HgException(e);
		}

	}

	public static Color getColorPreference(String pref) {
		RGB rgb = PreferenceConverter.getColor(MercurialEclipsePlugin.getDefault()
				.getPreferenceStore(), pref);
		return getColor(rgb);
	}

	public static Color getColor(RGB rgb) {
		Color color;
		synchronized (COLOR_MAP) {
			color = COLOR_MAP.get(rgb);
			if (color == null) {
				color = new Color(MercurialEclipsePlugin.getStandardDisplay(), rgb);
				COLOR_MAP.put(rgb, color);
			}
		}
		return color;
	}

	public static void disposeColorsAndFonts() {
		for (Color c : COLOR_MAP.values()) {
			c.dispose();
		}
		COLOR_MAP.clear();
		for (Font f : FONT_MAP.values()) {
			f.dispose();
		}
		FONT_MAP.clear();
	}

	public static Font getFont(FontData data) {
		Font font;
		synchronized (FONT_MAP) {
			font = FONT_MAP.get(data);
			if (font == null) {
				font = new Font(MercurialEclipsePlugin.getStandardDisplay(), data);
				FONT_MAP.put(data, font);
			}
		}
		return font;
	}

	public static Font getFontPreference(String pref) {
		FontData data = PreferenceConverter.getFontData(MercurialEclipsePlugin.getDefault()
				.getPreferenceStore(), pref);
		return getFont(data);
	}

	/**
	 * Get the parent revision. Is aware of file renames.
	 *
	 * Note: May query and update the file status of cs.
	 *
	 * @param cs The changeset to query for
	 * @param file The file as of cs's revision
	 * @return Storage for the parent revision
	 * @throws HgException
	 */
	public static MercurialRevisionStorage getParentRevision(ChangeSet cs, IFile file) throws HgException {
		String[] parents = cs.getParents();
		if(cs.getRevision().getRevision() == 0){
			return new NullRevision(file, cs);
		} else if (parents.length == 0) {
			// TODO for some reason, we do not always have right parent info in the changesets
			// If we are on the different branch then the changeset? or if the changeset
			// logs was created for a file, and not each version of a *file* has
			// direct version predecessor. So such tree 20 -> 21 -> 22 works fine,
			// but tree 20 -> 22 seems not to work per default
			// So simply enforce the parents resolving
			try {
				parents = HgParentClient.getParentNodeIds(file, cs);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
			if (parents.length == 0) {
				return new NullRevision(file, cs);
			}
		}

		if (!cs.hasFileStatus()) {
			ChangeSet newCs = HgLogClient.getChangeset(cs.getHgRoot(), cs.getChangeset(), true);

			if (newCs != null) {
				assert newCs.hasFileStatus();
				cs.setChangedFiles(newCs.getChangedFiles());
				assert cs.hasFileStatus();
			}
		}

		FileStatus stat = cs.getStatus(file);

		if (stat == null) {
			// TODO: throw an exception instead?
			MercurialEclipsePlugin.logWarning("Cannot correctly calculate parent changeset", new IllegalStateException());
		} else if (stat.isCopied()) {
			file = ResourceUtils.getFileHandle(stat.getAbsoluteCopySourcePath());
		}

		return new MercurialRevisionStorage(file, parents[0]);
	}

	public static void setMergeViewDialogShown(boolean shown) {
		try {
			ResourcesPlugin
					.getWorkspace()
					.getRoot()
					.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED,
							shown ? "true" : null);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public static boolean isMergeViewDialogShown() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot()
					.getSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED) != null;
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return true;
		}
	}
}
