/*******************************************************************************
 * Copyright (c) 2010 Bastian Doetsch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *     Andrei Loskutov - bug fixes
 *     Ilya Ivanov (Intland) - modifications
 *     Zsolt Koppany (Intland)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.dialogs.NewHeadsDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author Bastian
 */
public class UpdateJob extends Job {
	private final HgRoot hgRoot;
	private final boolean cleanEnabled;
	private final String revision;
	private boolean handleCrossBranches = false;

	/**
	 * Job to do a working directory update to the specified version.
	 * @param revision the target revision
	 * @param cleanEnabled if true, discard all local changes.
	 * @param handleCrossBranches
	 */
	public UpdateJob(String revision, boolean cleanEnabled, HgRoot hgRoot, boolean handleCrossBranches) {
		super("Updating working directory");
		this.hgRoot = hgRoot;
		this.cleanEnabled = cleanEnabled;
		this.revision = revision;
		this.handleCrossBranches = handleCrossBranches;
	}

	public UpdateJob(String revision, boolean cleanEnabled, HgRoot hgRoot) {
		this(revision, cleanEnabled, hgRoot, false);
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		String jobName = "Updating " + hgRoot.getName();
		if (revision != null && revision.length()>0) {
			jobName += " to revision " + revision;
		}
		if (cleanEnabled) {
			jobName += " discarding all local changes (-C option).";
		}
		monitor.beginTask(jobName, 2);
		monitor.subTask("Calling Mercurial...");
		monitor.worked(1);
		try {
			HgUpdateClient.update(hgRoot, revision, cleanEnabled);
			monitor.worked(1);

			// if revision != null then it's an update operation to particular change set,
			// don't need to handle cross branches in this case
			if (MergeHandler.getHeadsInCurrentBranch(hgRoot).size() > 1
					&& revision == null && handleCrossBranches) {
				handleMultipleHeads(hgRoot, cleanEnabled);
			}
		} catch (HgException e) {
			if (e.getMessage().contains("abort: crosses branches") && e.getStatus().getCode() == -1 && handleCrossBranches) {
				// don't log this error because it's a common situation and can be handled
				handleMultipleHeads(hgRoot, cleanEnabled);
				return new Status(IStatus.OK, MercurialEclipsePlugin.ID, "Update canceled - merge needed");
			}
			MercurialEclipsePlugin.logError(e);
			return e.getStatus();
		} finally {
			monitor.done();
		}
		return new Status(IStatus.OK, MercurialEclipsePlugin.ID, "Update to revision " + revision + " succeeded.");
	}

	public static void handleMultipleHeads(final HgRoot root, final boolean clean) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				NewHeadsDialog dialog;
				try {
					dialog = new NewHeadsDialog(Display.getDefault().getActiveShell(), root);
					dialog.setClean(clean);
//					dialog.setBlockOnOpen(true);
					dialog.open();
				} catch (HgException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
			}
		});
	}

}