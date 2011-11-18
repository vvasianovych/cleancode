/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * @author zingo, Jerome Negre <jerome+hg@jnegre.org>
 */
public class CompareAction extends SingleFileAction {

	private boolean mergeEnabled;
	private ISynchronizePageConfiguration syncConfig;
	private boolean isUncommittedCompare = false;

	// constructors

	/**
	 * Empty constructor must be here, otherwise Eclipse wouldn't be able to create the object via reflection
	 */
	public CompareAction() {
		super();
	}

	public CompareAction(IFile file) {
		this();
		this.selection = file;
	}

	// operations

	/**
	 * @param file non null
	 */
	@Override
	protected void run(final IFile file) throws TeamException {
		Job job = new Job("Diff for " + file.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				boolean workspaceUpdateConflict = isUncommittedCompare
						&& MercurialStatusCache.getInstance().isWorkspaceUpdateConfict(file);

				if (workspaceUpdateConflict) {
					// This job shouldn't run in the UI thread despite needing it a lot because it
					// invokes hg multiple times.
					final Boolean[] resultRef = { Boolean.FALSE };
					getShell().getDisplay().syncExec(new Runnable() {
						public void run() {
							resultRef[0] = Boolean
									.valueOf(MessageDialog
											.openQuestion(
													getShell(),
													"Compare",
													"This file is in conflict. Would you like to use resolve to *RESTART* the merge with 3-way differences? Warning: if resolve fails this operations can potentially lose changes. If in doubt select 'No'."));
						}
					});

					workspaceUpdateConflict = resultRef[0].booleanValue();
				}

				if (mergeEnabled || workspaceUpdateConflict) {
					openMergeEditor(file, workspaceUpdateConflict);
					return Status.OK_STATUS;
				}
				boolean clean = MercurialStatusCache.getInstance().isClean(file);
				if (!clean) {
					compareToLocal(file);
					return Status.OK_STATUS;
				}
				try {
					ChangeSet cs = LocalChangesetCache.getInstance().getChangesetByRootId(file);

					if (cs != null) {
						CompareUtils.openEditor(file, MercurialUtilities.getParentRevision(cs, file), false, null);
						return Status.OK_STATUS;
					}
				} catch (TeamException e) {
					MercurialEclipsePlugin.logError(e);
				}
				// something went wrong. So compare to local
				compareToLocal(file);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void compareToLocal(IFile file) {
		try {
			CompareUtils.openEditor(file, new MercurialRevisionStorage(file), false, syncConfig);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * @param file non null
	 */
	private void openMergeEditor(final IFile file, boolean workspaceUpdateConflict){
		try {
			MercurialRevisionStorage ancestorNode;
			MercurialRevisionStorage mergeNode;

			if (workspaceUpdateConflict) {
				String[] changeSets = HgResolveClient.restartMergeAndGetChangeSetsForCompare(file);
				String otherId = changeSets[1];
				String ancestorId = changeSets[2];

				if (otherId == null || ancestorId == null) {

					getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							MessageDialog.openError(getShell(), "Merge error",
									"Couldn't retrieve merge info from Mercurial");
						}
					});

					MercurialEclipsePlugin.logError(new HgException("HgResolveClient returned null revision id"));
					return;
				}

				mergeNode = new MercurialRevisionStorage(file, otherId);
				ancestorNode = new MercurialRevisionStorage(file, ancestorId);
			} else {
				HgRoot hgRoot = MercurialTeamProvider.getHgRoot(file);
				if(hgRoot == null) {
					MercurialEclipsePlugin.showError(new IllegalStateException(
							"Failed to find hg root for: " + file.getLocation()));
					return;
				}
				String mergeNodeId = MercurialStatusCache.getInstance().getMergeChangesetId(hgRoot);
				String[] parents = HgParentClient.getParentNodeIds(hgRoot);
				int ancestor = HgParentClient.findCommonAncestor(hgRoot, parents[0], parents[1]);

				mergeNode = new MercurialRevisionStorage(file, mergeNodeId);
				ancestorNode = (ancestor <= 0) ? null : new MercurialRevisionStorage(file, ancestor);
			}

			final CompareEditorInput compareInput = CompareUtils.getPrecomputedCompareInput(file,
					ancestorNode, mergeNode);

			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					CompareUI.openCompareEditor(compareInput);
				}
			});
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	public void setEnableMerge(boolean enable) {
		mergeEnabled = enable;
	}

	public void setUncommittedCompare(boolean enable) {
		isUncommittedCompare = enable;
	}

	public void setSynchronizePageConfiguration(ISynchronizePageConfiguration syncConfig){
		this.syncConfig = syncConfig;
	}
}
