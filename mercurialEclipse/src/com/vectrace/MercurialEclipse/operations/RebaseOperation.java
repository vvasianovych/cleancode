/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.views.MergeView;

/**
 * @author bastian
 *
 * The operation doesn't fail if a rebase conflict occurs.
 */
public class RebaseOperation extends HgOperation {

	private final HgRoot hgRoot;
	private int sourceRev = -1;
	private int destRev = -1;
	private int baseRev = -1;
	private final boolean collapse;
	private final boolean abort;
	private final boolean cont;
	private boolean keepBranches;
	private boolean keep;
	private final String user;

	public RebaseOperation(IRunnableContext context, HgRoot hgRoot,
			int sourceRev, int destRev, int baseRev, boolean collapse,
			boolean abort, boolean cont) {
		this(context, hgRoot, sourceRev, destRev, baseRev, collapse, abort, cont, false, null);
	}

	protected RebaseOperation(IRunnableContext context, HgRoot hgRoot,
			int sourceRev, int destRev, int baseRev, boolean collapse,
			boolean abort, boolean cont, boolean keepBranches, String user) {
		super(context);
		this.hgRoot = hgRoot;
		this.sourceRev = sourceRev;
		this.destRev = destRev;
		this.baseRev = baseRev;
		this.collapse = collapse;
		this.abort = abort;
		this.cont = cont;
		this.keepBranches = keepBranches;
		this.user = user;
	}

	public void setKeep(boolean keep) {
		this.keep = keep;
	}

	public void setKeepBranches(boolean keepBranches) {
		this.keepBranches = keepBranches;
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(getActionDescription(), 2);
		boolean rebaseConflict = false;
		try {
			monitor.worked(1);
			monitor.subTask(Messages.getString("RebaseOperation.calling")); //$NON-NLS-1$
			boolean useExternalMergeTool = MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getBoolean(MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE);
			result = HgRebaseClient.rebase(hgRoot,
					sourceRev,
					baseRev, destRev, collapse, cont, abort, keepBranches, keep, useExternalMergeTool, user);
			monitor.worked(1);

		} catch (HgException e) {
			rebaseConflict = HgRebaseClient.isRebaseConflict(e);

			if(rebaseConflict) {
				result = e.getMessage();
			} else {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
		} finally {
			RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(hgRoot,
					RefreshRootJob.ALL);
			job.schedule();
			job.join();
			monitor.done();
			if(rebaseConflict) {
				showMergeView();
			}
		}
	}

	/**
	 * show Merge view, as it offers to abort a merge and revise the automatically merged files
	 */
	private void showMergeView() {
		Runnable runnable = new Runnable() {
			public void run() {
				MergeView view;
				try {
					view = (MergeView) MercurialEclipsePlugin.getActivePage()
							.showView(MergeView.ID);
					view.refresh(hgRoot);
				} catch (PartInitException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
			}
		};
		Display.getDefault().asyncExec(runnable);
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("RebaseOperation.rebasing"); //$NON-NLS-1$
	}

	/**
	 * Factory method to create a continue rebase operation
	 */
	public static RebaseOperation createContinue(IRunnableContext context, HgRoot root, String user)
	{
		return new RebaseOperation(context, root, -1, -1, -1, false, false, true, false, user);
	}

	/**
	 * Factory method to create a abort rebase operation
	 */
	public static RebaseOperation createAbort(IRunnableContext context, HgRoot root)
	{
		return new RebaseOperation(context, root, -1, -1, -1, false, true, false);
	}
}
