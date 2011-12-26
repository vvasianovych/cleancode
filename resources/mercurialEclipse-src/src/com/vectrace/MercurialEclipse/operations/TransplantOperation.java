/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient.TransplantOptions;
import com.vectrace.MercurialEclipse.dialogs.TransplantRejectsDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public class TransplantOperation extends HgOperation {
	private static final Pattern HAS_REJECTS_PATTERN = Pattern.compile("abort:.*run hg transplant --continue");
	private static final Pattern REJECT_PATTERN = Pattern.compile("saving rejects to file (.*)\\s");
	private static final Pattern CHANGESET_PATTERN = Pattern.compile("applying ([a-z0-9]*)\\s");

	private final TransplantOptions options;
	private final IHgRepositoryLocation repo;
	private final HgRoot hgRoot;

	public TransplantOperation(IRunnableContext context, HgRoot hgRoot, TransplantOptions options,
			IHgRepositoryLocation repo) {
		super(context);
		this.hgRoot = hgRoot;
		this.options = options;
		this.repo = repo;
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		monitor.beginTask("Transplanting changesets...", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

		// Timer which is used to monitor the monitor cancellation
		Timer t = new Timer("Transplant watcher", false);

		// only start timer if the operation is NOT running in the UI thread
		if(Display.getCurrent() == null){
			final Thread threadToCancel = Thread.currentThread();
			t.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (monitor.isCanceled() && !threadToCancel.isInterrupted()) {
						threadToCancel.interrupt();
					}
				}
			}, 500, 50);
		}

		try {
			try {
				result = HgTransplantClient.transplant(hgRoot, repo, options);
			} finally {
				RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(hgRoot,
						RefreshRootJob.ALL);
				job.schedule();
				job.join();
				t.cancel();
			}
		} catch (HgException e) {
			if (handleTransplantException(e)) {
				result = e.getMessage();
			} else {
				throw new InvocationTargetException(e);
			}
		}

		if (result != null && result.length() != 0) {
			HgClients.getConsole().printMessage(result, null);
		}
	}

	private boolean handleTransplantException(HgException e) {
		final String message = e.getMessage();
		if (!HAS_REJECTS_PATTERN.matcher(message).find()) {
			return false;
		}

		try {
			Matcher matcher;

			matcher = CHANGESET_PATTERN.matcher(message);
			boolean found = matcher.find() &&  matcher.groupCount() > 0;
			if(!found) {
				return false;
			}
			final String changeSetId = matcher.group(1);

			final ArrayList<IFile> rejects = new ArrayList<IFile>();

			matcher = REJECT_PATTERN.matcher(message);
			int lastMatchOffset = 0;
			while (matcher.find(lastMatchOffset) && matcher.groupCount() > 0) {
				String filename = matcher.group(1);
				IPath path = new Path(hgRoot.getPath()).append(filename);
				IFile file = FileBuffers.getWorkspaceFileAtLocation(path);
				if (file != null) {
					rejects.add(file);
				}
				lastMatchOffset = matcher.end();
			}

			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					new TransplantRejectsDialog(getShell(), hgRoot, changeSetId, rejects).open();
				}
			});

			return true;
		} catch (Exception ex) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
			return false;
		}
	}

	@Override
	protected String getActionDescription() {
		return "Transplanting to " + hgRoot.getName();
	}

	public static TransplantOperation createContinueOperation(IRunnableContext context, HgRoot hgRoot) {
		TransplantOptions options = new TransplantOptions();
		options.continueLastTransplant = true;
		return new TransplantOperation(context, hgRoot, options, null);
	}

	public IHgRepositoryLocation getRepo() {
		return repo;
	}
}