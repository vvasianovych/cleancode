/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

/**
 * @author bastian
 */
public abstract class BisectAbstractAction extends Action {

	private final MercurialHistoryPage mercurialHistoryPage;

	public BisectAbstractAction(String text, MercurialHistoryPage mercurialHistoryPage) {
		super(text);
		this.mercurialHistoryPage = mercurialHistoryPage;
	}


	public boolean isBisectStarted() {
		return mercurialHistoryPage.getMercurialHistory().isBisectStarted();
	}

	public void setBisectStarted(boolean started) {
		mercurialHistoryPage.getMercurialHistory().setBisectStarted(started);
	}

	private MercurialRevision getRevision() {
		MercurialRevision[] selectedRevisions = mercurialHistoryPage.getSelectedRevisions();
		if (selectedRevisions != null && selectedRevisions.length == 1) {
			return selectedRevisions[0];
		}
		ChangeSet cs = mercurialHistoryPage.getCurrentWorkdirChangeset();
		return (MercurialRevision) mercurialHistoryPage.getMercurialHistory().getFileRevision(
				cs.getChangeset());
	}

	abstract String callBisect(final HgRoot root, final ChangeSet cs) throws HgException;

	@Override
	public void run() {
		final HgRoot root;
		try {
			root = MercurialTeamProvider.getHgRoot(mercurialHistoryPage.resource);
			if(root == null || checkDirty(root)){
				return;
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
			return;
		}

		final MercurialRevision rev = getRevision();
		final ChangeSet cs = rev.getChangeSet();
		new Job(Messages.BisectAbstractAction_bisecting) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final String result = callBisect(root, cs);

					if (result.startsWith(Messages.BisectAbstractAction_successString)) {
						HgBisectClient.reset(root);
					}

					new RefreshWorkspaceStatusJob(root, RefreshRootJob.LOCAL).schedule();

					new SafeUiJob(Messages.BisectAbstractAction_showBisectionResult) {
						@Override
						protected IStatus runSafe(IProgressMonitor m) {
							if (result.length() > 0) {
								MercurialEclipsePlugin.logInfo(result, null);
								MessageDialog.openInformation(getDisplay().getActiveShell(),
										Messages.BisectAbstractAction_BisectionResult, result);
							}
							updateHistory(rev, root);
							return super.runSafe(m);
						}
					}.schedule();
				} catch (HgException e) {
					MercurialEclipsePlugin.showError(e);
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}


	protected boolean checkDirty(final HgRoot root) throws HgException {
		if (HgStatusClient.isDirty(root)) {
			MessageDialog.openWarning(mercurialHistoryPage.getControl().getShell(),
					"Uncommitted Changes", //$NON-NLS-1$
					"Your hg root has uncommited changes."
					+ "\nPlease commit or revert to start Bisection."); //$NON-NLS-1$
			return true;
		}
		return false;
	}

	protected void updateHistory(MercurialRevision rev, HgRoot root) {
		mercurialHistoryPage.clearSelection();
		mercurialHistoryPage.refresh();
	}
}