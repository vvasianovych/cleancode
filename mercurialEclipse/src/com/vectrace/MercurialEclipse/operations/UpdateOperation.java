/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class UpdateOperation extends HgOperation {

	private final String rev;
	private final boolean forest;
	private final boolean svn;
	private final HgRoot hgRoot;

	/**
	 * @param context
	 * @param rev the LAST revision which will be at the cloned repo, all subsequent revisions will be present
	 * @param forest true to use forest extension
	 * @param svn to use svn extension
	 */
	public UpdateOperation(IRunnableContext context, HgRoot hgRoot, String rev, boolean forest, boolean svn) {
		super(context);
		this.hgRoot = hgRoot;
		this.rev = rev;
		this.forest = forest;
		this.svn = svn;
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IProgressMonitor m) throws InvocationTargetException,
			InterruptedException {

		m.beginTask(Messages.getString("CloneRepoWizard.updateOperation.name"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

		// Timer which is used to monitor the moniktor cancellation
		Timer t = new Timer("Update watcher", false);

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
				throw new IllegalArgumentException("SVN update not supported yet!");
			} else if (forest) {
				throw new IllegalArgumentException("Forest update not supported yet!");
			} else {
				HgUpdateClient.update(hgRoot, rev, true);
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
		return "Updating " + hgRoot.getName() + " to selected revision";
	}

}