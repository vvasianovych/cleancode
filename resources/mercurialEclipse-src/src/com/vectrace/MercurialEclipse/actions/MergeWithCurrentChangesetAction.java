/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.history.MercurialHistoryPage;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author Bastian
 *
 */
public class MergeWithCurrentChangesetAction extends Action {
	private final MercurialHistoryPage mhp;
	private static final ImageDescriptor IMAGE_DESC = MercurialEclipsePlugin
			.getImageDescriptor("actions/merge.gif"); //$NON-NLS-1$

	/**
	 *
	 */
	public MergeWithCurrentChangesetAction(MercurialHistoryPage mhp) {
		super(Messages.getString("MergeWithCurrentChangesetAction.mergeSelectedChangesetWithCurrentChangeset"), IMAGE_DESC); //$NON-NLS-1$
		this.mhp = mhp;
	}

	@Override
	public void run() {
		final MercurialRevision rev = getRevision();
		new SafeWorkspaceJob(Messages.getString("MergeWithCurrentChangesetAction.0") + rev.getContentIdentifier() //$NON-NLS-1$
				+ Messages.getString("MergeWithCurrentChangesetAction.1")) { //$NON-NLS-1$
			@Override
			protected org.eclipse.core.runtime.IStatus runSafe(
					org.eclipse.core.runtime.IProgressMonitor monitor) {
				try {
					monitor.beginTask(Messages.getString("MergeWithCurrentChangesetAction.2") + rev.getContentIdentifier() //$NON-NLS-1$
							+ Messages.getString("MergeWithCurrentChangesetAction.3"), 3); //$NON-NLS-1$
					monitor.subTask(Messages.getString("MergeWithCurrentChangesetAction.4")); //$NON-NLS-1$
					final HgRoot root = MercurialTeamProvider.getHgRoot(rev.getResource());
					if(root == null) {
						MercurialEclipsePlugin.logError(new IllegalStateException("Hg root not found for: " + rev));
						return Status.CANCEL_STATUS;
					}
					monitor.worked(1);
					monitor.subTask(Messages.getString("MergeWithCurrentChangesetAction.5")); //$NON-NLS-1$
					new SafeUiJob("Merging...") { //$NON-NLS-1$
						@Override
						protected IStatus runSafe(IProgressMonitor m) {
							Shell activeShell = getDisplay().getActiveShell();
							try {
								if (HgStatusClient.isDirty(root)) {
									if (!MessageDialog
											.openQuestion(activeShell,
													Messages.getString("MergeWithCurrentChangesetAction.6"), //$NON-NLS-1$
													Messages.getString("MergeWithCurrentChangesetAction.7"))) { //$NON-NLS-1$
										return super.runSafe(m);
									}
								}
								MergeHandler.mergeAndCommit(root, activeShell, m, true, rev
										.getChangeSet(), true);
							} catch (Exception e) {
								MercurialEclipsePlugin.logError(e);
								MercurialEclipsePlugin.showError(e);
							}
							return super.runSafe(m);
						}
					}.schedule();
				} catch (Exception e) {
					MercurialEclipsePlugin.logError(e);
					MercurialEclipsePlugin.showError(e);
				}
				monitor.done();
				return super.runSafe(monitor);
			}
		}.schedule();
		super.run();
	}

	/**
	 * @return
	 */
	private MercurialRevision getRevision() {
		MercurialRevision[] selectedRevisions = mhp.getSelectedRevisions();
		if (selectedRevisions != null && selectedRevisions.length == 1) {
			return selectedRevisions[0];
		}
		ChangeSet cs = mhp.getCurrentWorkdirChangeset();
		return (MercurialRevision) mhp.getMercurialHistory().getFileRevision(cs.getChangeset());
	}
}
