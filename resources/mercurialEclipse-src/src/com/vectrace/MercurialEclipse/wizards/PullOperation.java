/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSvnClient;
import com.vectrace.MercurialEclipse.commands.extensions.forest.HgFpushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation.BundleRepository;

class PullOperation extends HgOperation {
	private final boolean doUpdate;

	private final HgRoot hgRoot;
	private final IHgRepositoryLocation repo;
	private final boolean force;
	private final ChangeSet pullRevision;
	private final boolean timeout;
	private final boolean merge;
	private String output = ""; //$NON-NLS-1$
	private final boolean showCommitDialog;
	private final File bundleFile;
	private final boolean forest;
	private final File snapFile;
	private final boolean rebase;
	private final boolean svn;
	private final boolean doCleanUpdate;

	public PullOperation(IRunnableContext context, boolean doUpdate,
			boolean doCleanUpdate, HgRoot hgRoot, boolean force, IHgRepositoryLocation repo,
			ChangeSet pullRevision, boolean timeout, boolean merge,
			boolean showCommitDialog, File bundleFile, boolean forest,
			File snapFile, boolean rebase, boolean svn) {
		super(context);
		this.doUpdate = doUpdate;
		this.doCleanUpdate = doCleanUpdate;
		this.hgRoot = hgRoot;
		this.force = force;
		this.repo = repo;
		this.pullRevision = pullRevision;
		this.timeout = timeout;
		this.merge = merge;
		this.showCommitDialog = showCommitDialog;
		this.bundleFile = bundleFile;
		this.forest = forest;
		this.snapFile = snapFile;
		this.rebase = rebase;
		this.svn = svn;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("PullRepoWizard.pullOperation.description"); //$NON-NLS-1$
	}

	private String performMerge(IProgressMonitor monitor) throws HgException, InterruptedException {
		String r = Messages.getString("PullRepoWizard.pullOperation.mergeHeader"); //$NON-NLS-1$
		monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.merging")); //$NON-NLS-1$
		if (HgLogClient.getHeads(hgRoot).length > 1) {

			SafeUiJob job = new SafeUiJob(Messages.getString("PullRepoWizard.pullOperation.mergeJob.description")) { //$NON-NLS-1$
				@Override
				protected IStatus runSafe(IProgressMonitor m) {
					try {
						String res = MergeHandler.determineMergeHeadAndMerge(hgRoot, getShell(), m, true, showCommitDialog);
						return new Status(IStatus.OK, MercurialEclipsePlugin.ID, res);
					} catch (CoreException e) {
						MercurialEclipsePlugin.logError(e);
						return e.getStatus();
					}
				}
			};
			job.schedule();
			job.join();
			IStatus jobResult = job.getResult();
			if (jobResult.getSeverity() == IStatus.OK) {
				r += jobResult.getMessage();
			} else {
				throw new HgException(jobResult);
			}
		}
		monitor.worked(1);
		return r;
	}

	private String performPull(final IHgRepositoryLocation repository,
			IProgressMonitor monitor) throws CoreException {
		monitor.worked(1);
		monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.incoming")); //$NON-NLS-1$
		String r = Messages.getString("PullRepoWizard.pullOperation.pull.header"); //$NON-NLS-1$
		boolean updateSeparately = false;

		if (svn) {
			r += HgSvnClient.pull(hgRoot);
			if (rebase) {
				r += HgSvnClient.rebase(hgRoot);
			}
		} else if (bundleFile == null) {
			if (forest) {
				File forestRoot = hgRoot.getParentFile();
				r += HgFpushPullClient.fpull(forestRoot, repo,
						doUpdate, timeout, pullRevision, true, snapFile, false);
			} else {
				if (doUpdate) {
					updateSeparately = true;
				}
				r += HgPushPullClient.pull(hgRoot, pullRevision, repo, false, rebase, force, timeout, merge);
			}
		} else {
			if (doUpdate) {
				updateSeparately = true;
			}
			File canonicalBundle = toCanonicalBundle();
			BundleRepository bundleRepo = new BundleRepository(canonicalBundle);
			r += HgPushPullClient.pull(hgRoot, pullRevision, bundleRepo, false, rebase, force, timeout, merge);
		}

		monitor.worked(1);
		saveRepo(monitor);

		if (updateSeparately) {
			runUpdate();
		}
		return r;

	}

	private File toCanonicalBundle() throws HgException {
		File canonicalFile = null;
		try {
			canonicalFile = bundleFile.getCanonicalFile();
		} catch (IOException e) {
			String message = "Failed to get canonical bundle path for: " + bundleFile;
			MercurialEclipsePlugin.logError(message, e);
			throw new HgException(message, e);
		}
		return canonicalFile;
	}

	private void runUpdate() {
		new Job("Hg update after pull") {
			@Override
			public IStatus run(IProgressMonitor monitor1) {
				try {
					// if merge or rebase requested don't ask user what to do
					// with cross branches
					boolean handleCrossBranches = !(merge || rebase);
					UpdateHandler updateHandler = new UpdateHandler(handleCrossBranches);
					updateHandler.setCleanEnabled(doCleanUpdate);
					updateHandler.run(hgRoot);
					return Status.OK_STATUS;
				} catch (HgException e) {
					// no point in complaining, since they want to merge/rebase anyway
					if ((merge || rebase) && e.getMessage().contains("crosses branches")) {
						return Status.OK_STATUS;
					}
					MercurialEclipsePlugin.logError(e);
					MercurialEclipsePlugin.showError(e);
					return Status.CANCEL_STATUS;
				}
			}
		}.schedule();
	}

	private boolean saveRepo(IProgressMonitor monitor) {
		// It appears good. Stash the repo location.
		monitor.subTask(Messages.getString("PullRepoWizard.pullOperation.addRepo") + repo); //$NON-NLS-1$
		try {
			MercurialEclipsePlugin.getRepoManager().addRepoLocation(hgRoot, repo);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(Messages
					.getString("PullRepoWizard.addingRepositoryFailed"), e); //$NON-NLS-1$
		}
		monitor.worked(1);
		return true;
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			monitor.beginTask(Messages.getString("PullRepoWizard.pullOperation.pulling"), 6); //$NON-NLS-1$
			output += performPull(repo, monitor);
			if (merge) {
				output += performMerge(monitor);
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e, e.getMessage());
		}
		monitor.done();
	}

	public String getOutput() {
		return output;
	}
}