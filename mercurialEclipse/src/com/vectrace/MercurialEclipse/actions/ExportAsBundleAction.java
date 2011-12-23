/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 * 		Andrei Loskutov 	- bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBundleClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistoryPage;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author Bastian
 */
public class ExportAsBundleAction extends Action {

	private final class ExportBundleJob extends Job {
		private final MercurialRevision rev;

		private ExportBundleJob(String name, MercurialRevision rev) {
			super(name);
			this.rev = rev;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				monitor
						.beginTask(
								Messages.getString("ExportAsBundleAction.exportingRevision") + rev.getContentIdentifier() //$NON-NLS-1$
										+ Messages.getString("ExportAsBundleAction.toBundle"), 3); //$NON-NLS-1$
				monitor.subTask(Messages
						.getString("ExportAsBundleAction.determiningRepositoryRoot")); //$NON-NLS-1$
				HgRoot root = MercurialTeamProvider.getHgRoot(rev.getResource());
				if(root == null) {
					MercurialEclipsePlugin.logError(new IllegalStateException("Hg root not found for: " + rev));
					return Status.CANCEL_STATUS;
				}
				monitor.worked(1);
				monitor.subTask(Messages.getString("ExportAsBundleAction.callingMercurial")); //$NON-NLS-1$

				determineFileAndBase();

				if (file == null) {
					// user cancel
					monitor.setCanceled(true);
					return Status.CANCEL_STATUS;
				}
				HgBundleClient.bundle(root, rev.getChangeSet(), null, file, false, base);
				monitor.worked(1);

				final String message = Messages.getString("ExportAsBundleAction.theRevision") //$NON-NLS-1$
						+ rev.getContentIdentifier()
						+ Messages
								.getString("ExportAsBundleAction.andAllPreviousRevisionsHaveBeenExported") //$NON-NLS-1$
						+ file;

				showMessage(message);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
				MercurialEclipsePlugin.showError(e);
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}

	}

	private final MercurialHistoryPage mhp;
	private String file;
	private String base;
	private static final ImageDescriptor IMAGE_DESC = MercurialEclipsePlugin
			.getImageDescriptor("export.gif"); //$NON-NLS-1$

	private void determineFileAndBase() {
		final Display display = MercurialEclipsePlugin.getStandardDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				Shell shell = MercurialEclipsePlugin.getActiveShell();
				FileDialog fileDialog = new FileDialog(shell);
				fileDialog.setText(Messages
						.getString("ExportAsBundleAction.pleaseEnterTheNameOfTheBundleFile")); //$NON-NLS-1$
				file = fileDialog.open();
				if(file == null){
					// user cancel
					return;
				}
				InputDialog d = new InputDialog(shell, "Please specify the base revision",
						"Please specify the base revision e.g. 1333", "0", null);
				d.open();
				base = d.getValue();
			}
		});
	}

	private static void showMessage(final String message) {
		final Display display = MercurialEclipsePlugin.getStandardDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(null, Messages
						.getString("ExportAsBundleAction.createdSuccessfully"), message); //$NON-NLS-1$

			}
		});
	}

	public ExportAsBundleAction(MercurialHistoryPage mhp) {
		super(Messages.getString("ExportAsBundleAction.exportSelectedRevisionAsBundle"), IMAGE_DESC);
		this.mhp = mhp;
	}

	@Override
	public void run() {
		final MercurialRevision rev = getRevision();
		if (rev == null) {
			return;
		}
		new ExportBundleJob(Messages.getString("ExportAsBundleAction.exportingRevision")
				+ rev.getContentIdentifier() + Messages.getString("ExportAsBundleAction.toBundle"),
				rev).schedule();
	}

	private MercurialRevision getRevision() {
		MercurialRevision[] selectedRevisions = mhp.getSelectedRevisions();
		if (selectedRevisions != null && selectedRevisions.length == 1) {
			return selectedRevisions[0];
		}
		ChangeSet cs = mhp.getCurrentWorkdirChangeset();
		return (MercurialRevision) mhp.getMercurialHistory().getFileRevision(cs.getChangeset());
	}
}
