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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgCloneClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSvnClient;
import com.vectrace.MercurialEclipse.commands.extensions.forest.HgFcloneClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class CloneOperation extends HgOperation {

	private final File parentDirectory;
	private final IHgRepositoryLocation repo;
	private final boolean noUpdate;
	private final boolean pull;
	private final boolean uncompressed;
	private final boolean timeout;
	private final String rev;
	private final String cloneName;
	private final boolean forest;
	private final boolean svn;

	/**
	 *
	 * @param context
	 * @param parentDirectory the root directory for the cloned repository
	 * @param repo non null repository to clone from
	 * @param noUpdate true to clone only the repo (.hg) and do not create a working copy (it will be at version 0)
	 * @param pull true to use pull protocoll to copy metadata
	 * @param uncompressed true to NOT compress the data
	 * @param timeout
	 * @param rev the LAST revision which will be at the cloned repo, all subsequent revisions will be present
	 * @param cloneName the base name of new repository. If null, basename of remote will be used
	 * @param forest true to use forest extension
	 * @param svn to use svn extension
	 */
	public CloneOperation(IRunnableContext context, File parentDirectory,
			IHgRepositoryLocation repo, boolean noUpdate, boolean pull,
			boolean uncompressed, boolean timeout, String rev,
			String cloneName, boolean forest, boolean svn) {
		super(context);
		this.parentDirectory = parentDirectory;
		this.repo = repo;
		this.noUpdate = noUpdate;
		this.pull = pull;
		this.uncompressed = uncompressed;
		this.timeout = timeout;
		this.rev = rev;
		this.cloneName = cloneName;
		this.forest = forest;
		this.svn = svn;
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IProgressMonitor m) throws InvocationTargetException,
			InterruptedException {

		m.beginTask(Messages.getString("CloneRepoWizard.operation.name"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

		// Timer which is used to monitor the moniktor cancellation
		Timer t = new Timer("Clone watcher", false);

		// only start timer if the operation is NOT running in the UI thread
		if(Display.getCurrent() == null){
			final Thread threadToCancel = Thread.currentThread();
			t.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (m.isCanceled() && !threadToCancel.isInterrupted()) {
						threadToCancel.interrupt();
					}
				}
			}, 1000, 50);
		}
		try {
			if (svn) {
				HgSvnClient.clone(parentDirectory, repo, timeout, cloneName);
			} else if (!forest) {
				HgCloneClient.clone(parentDirectory, repo, noUpdate, pull,
						uncompressed, timeout, rev, cloneName);
			} else {
				HgFcloneClient.fclone(parentDirectory, repo, noUpdate, pull,
						uncompressed, timeout, rev, cloneName);
			}
			m.worked(1);
		} catch (HgException e) {
			if(e.getCause() instanceof InterruptedException){
				throw (InterruptedException) e.getCause();
			}
			throw new InvocationTargetException(e);
		} finally {
			t.cancel();
		}
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("CloneRepoWizard.actionDescription.1") + repo + Messages.getString("CloneRepoWizard.actionDescription.2") + cloneName; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public File getParentDirectory() {
		return parentDirectory;
	}

}