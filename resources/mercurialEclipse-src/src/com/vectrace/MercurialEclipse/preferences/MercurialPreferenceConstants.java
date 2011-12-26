/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Ahlberg - implementation
 *     Jérôme Nègre   - constants are now, well, constant
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public final class MercurialPreferenceConstants {

	private MercurialPreferenceConstants() {
		// hide constructor of utility class.
	}

	// executables
	public static final String MERCURIAL_EXECUTABLE = "hg"; //$NON-NLS-1$
	public static final String USE_BUILT_IN_HG_EXECUTABLE = "hg.useBuiltInHgExecutable"; //$NON-NLS-1$
	public static final String GPG_EXECUTABLE = "hg.gpg.executable"; //$NON-NLS-1$

	// user name should be per project in the future, different repositories
	// could have different names (sub optimal I know but it really could)
	public static final String MERCURIAL_USERNAME = "user.name"; //$NON-NLS-1$

	// label decorator
	public static final String LABELDECORATOR_LOGIC = "hg.labeldecorator.logic"; //$NON-NLS-1$
	public static final String LABELDECORATOR_LOGIC_2MM = "2-means-modified"; //$NON-NLS-1$
	public static final String LABELDECORATOR_LOGIC_HB = "high-bit"; //$NON-NLS-1$
	public static final String RESOURCE_DECORATOR_SHOW_CHANGESET = "hg.performance.fileShowsChangeset"; //$NON-NLS-1$
	public static final String RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET = "hg.performance.fileShowsChangesetIncoming"; //$NON-NLS-1$
	public static final String PREF_DECORATE_WITH_COLORS = "hg.labeldecorator.colors"; //$NON-NLS-1$
	public static final String PREF_AUTO_SHARE_PROJECTS = "hg.autoshare"; //$NON-NLS-1$

	/** true to automatically include all projects from the repository in the synchronize view */
	public static final String PREF_SYNC_ALL_PROJECTS_IN_REPO = "hg.syncAllProjectsInRepository"; //$NON-NLS-1$
	public static final String PREF_SYNC_ONLY_CURRENT_BRANCH = "hg.syncOnlyCurrentBranch"; //$NON-NLS-1$
	public static final String PREF_SYNC_PRESENTATION_MODE = "hg.syncPresentationMode";  //$NON-NLS-1$

	public static final String PREF_ENABLE_SUBREPO_SUPPORT = "enableSubrepoSupport"; //$NON-NLS-1$

	/** do not limit graphical log data to show pretty revision graphs in the history view */
	public static final String ENABLE_FULL_GLOG = "hg.performance.enableFullGlog"; //$NON-NLS-1$
	public static final String PREF_SIGCHECK_IN_HISTORY = Messages.getString("MercurialPreferenceConstants.sigcheck.in.history"); //$NON-NLS-1$

	// Timeouts
	public static final String DEFAULT_TIMEOUT = "hg.timeout.default"; //$NON-NLS-1$
	public static final String CLONE_TIMEOUT = "hg.timeout.clone"; //$NON-NLS-1$
	public static final String PUSH_TIMEOUT = "hg.timeout.push"; //$NON-NLS-1$
	public static final String PULL_TIMEOUT = "hg.timeout.pull"; //$NON-NLS-1$
	public static final String UPDATE_TIMEOUT = "hg.timeout.update"; //$NON-NLS-1$
	public static final String COMMIT_TIMEOUT = "hg.timeout.commit"; //$NON-NLS-1$
	public static final String IMERGE_TIMEOUT = "hg.timeout.imerge"; //$NON-NLS-1$
	public static final String LOG_TIMEOUT = "hg.timeout.log"; //$NON-NLS-1$
	public static final String STATUS_TIMEOUT = "hg.timeout.status"; //$NON-NLS-1$
	public static final String ADD_TIMEOUT = "hg.timeout.add"; //$NON-NLS-1$
	public static final String REMOVE_TIMEOUT = "hg.timeout.remove"; //$NON-NLS-1$

	// batch sizes
	public static final String LOG_BATCH_SIZE = "hg.batchsize.log"; //$NON-NLS-1$
	public static final String STATUS_BATCH_SIZE = "hg.batchsize.status"; //$NON-NLS-1$
	public static final String COMMIT_MESSAGE_BATCH_SIZE = "hg.batchsize.commitmessage"; //$NON-NLS-1$

	// remote performance: for incoming/outgoing, compute add/remove/modified status on changeset files
	public static final String SYNC_COMPUTE_FULL_REMOTE_FILE_STATUS = "hg.computeFullRemoteFileStatus"; //$NON-NLS-1$

	// console
	public static final String PREF_CONSOLE_SHOW_ON_MESSAGE = "hg.console.showOnMessage"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_WRAP = "hg.console.wrap"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_WIDTH = "hg.console.width"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_LIMIT_OUTPUT = "hg.console.limitOutput"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_HIGH_WATER_MARK = "hg.console.highWaterMark"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_COMMAND_COLOR = "hg.console.command_color"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_MESSAGE_COLOR = "hg.console.message_color"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_ERROR_COLOR = "hg.console.error_color"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_FONT = "hg.console.font"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_DEBUG = "hg.console.debug"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_DEBUG_TIME = "hg.console.debug.time"; //$NON-NLS-1$
	public static final String PREF_CONSOLE_SHOW_ON_STARTUP = "hg.console.showOnStartup"; //$NON-NLS-1$

	// merge
	public static final String PREF_USE_EXTERNAL_MERGE = "hg.merge.useExternal"; //$NON-NLS-1$

	// history view
	public static final String PREF_SHOW_COMMENTS = "pref_show_comments"; //$NON-NLS-1$
	public static final String PREF_SHOW_DIFFS = "pref_show_diffs"; //$NON-NLS-1$

	/** shows all tags also for file history, even if the file changeset's was not tagged as such */
	public static  final String PREF_SHOW_ALL_TAGS = "pref_show_alltags"; //$NON-NLS-1$
	public static final String PREF_WRAP_COMMENTS = "pref_wrap_comments"; //$NON-NLS-1$
	public static final String PREF_SHOW_PATHS = "pref_show_paths"; //$NON-NLS-1$
	public static final String PREF_SHOW_GOTO_TEXT = "pref_show_goto_text"; //$NON-NLS-1$
	public static final String PREF_AFFECTED_PATHS_LAYOUT = "pref_affected_paths_layout2"; //$NON-NLS-1$
	public static final int LAYOUT_HORIZONTAL = 1;
	public static final int LAYOUT_VERTICAL = 2;
	public static final String PREF_HISTORY_MERGE_CHANGESET_BACKGROUND = "hg.history.mergeChangesetsBackground";
	public static final String PREF_HISTORY_MERGE_CHANGESET_FOREGROUND = "hg.history.mergeChangesetsForeground";
	public static final String PREF_USE_MERCURIAL_USERNAME = "hg.useMercurialUsernameForCommit";
	public static final String PREF_DEFAULT_REBASE_KEEP_BRANCHES = "hg.defaulRebaseKeepBranches";
	public static final String PREF_DEFAULT_TRANSPLANT_FROM_LOCAL_BRANCHES = "hg.defaulTransplantFromLocal";

	// clone
	public static final String PREF_CLONE_UNCOMPRESSED = "hg.cloneUncompressed"; //$NON-NLS-1$

	// push
	public static final String PREF_PUSH_NEW_BRANCH = "hg.pushNewBranch"; //$NON-NLS-1$

	// dialogs
	public static final String PREF_SHOW_MULTIPLE_PROJECTS_DIALOG = "hg.showMultipleProjectsDialog";
	public static final String PREF_SHOW_PULL_WARNING_DIALOG = "hg.showPullWarningDialog";
	public static final String PREF_PRESELECT_UNTRACKED_IN_COMMIT_DIALOG = "hg.dialog.commit.preSelectUntracked";

	// security
	public static final String PREF_VERIFY_SERVER_CERTIFICATE = "hg.verifyServerCertificate"; //$NON-NLS-1$


}
