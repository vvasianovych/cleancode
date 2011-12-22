/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Ilya Ivanov (Intland) -	implementation
 * 		Andrei Loskutov       -	bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRevertClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.views.MergeView;
import com.vectrace.MercurialEclipse.wizards.RebaseWizard;

public class NewHeadsDialog extends IconAndMessageDialog  {

	private final static int MERGE_ID = IDialogConstants.CLIENT_ID + 1;
	private final static int REBASE_ID = IDialogConstants.CLIENT_ID + 2;
	private final static int SWITCH_ID = IDialogConstants.CLIENT_ID + 3;

	private final HgRoot hgRoot;
	private boolean moreThanTwoHeads;
	private boolean cleanUpdateRequested;

	public NewHeadsDialog(Shell parentShell, HgRoot hgRoot) throws HgException {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.CLOSE);

		int extraHeads = MergeHandler.getHeadsInCurrentBranch(hgRoot).size();
		if (extraHeads == 2) {
			message = Messages.getString("NewHeadsDialog.twoHeads");
		} else if (extraHeads > 2) {
			message = Messages.getString("NewHeadsDialog.manyHeads");
			moreThanTwoHeads = true;
		} else {
			throw new HgException("Should have at least two heads");
		}

		this.hgRoot = hgRoot;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.getString("NewHeadsDialog.title")); //$NON-NLS-1$
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		return createMessageArea(parent);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (moreThanTwoHeads) {
//			createButton(parent, SWITCH_ID, Messages.getString("NewHeadsDialog.manyHeads.Switch"), false);
			createButton(parent, REBASE_ID, Messages.getString("NewHeadsDialog.manyHeads.Rebase"), false);
			createButton(parent, MERGE_ID, Messages.getString("NewHeadsDialog.manyHeads.Merge"), false);
		} else {
//			createButton(parent, SWITCH_ID, Messages.getString("NewHeadsDialog.twoHeads.Switch"), false);
			createButton(parent, REBASE_ID, Messages.getString("NewHeadsDialog.twoHeads.Rebase"), false);
			createButton(parent, MERGE_ID, Messages.getString("NewHeadsDialog.twoHeads.Merge"), false);
		}
		createButton(parent, IDialogConstants.CANCEL_ID, Messages.getString("NewHeadsDialog.CancelUpdate"), false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case MERGE_ID:
			mergePressed();
			break;
		case REBASE_ID:
			rebasePressed();
			break;
		case SWITCH_ID:
			switchPressed();
			break;
		}
		close();	// every button closes the dialog
	}

	private void switchPressed() {

	}

	private void rebasePressed() {
		if (!clearAndContinue()) {
			return;
		}

		if (moreThanTwoHeads) {
			RebaseWizard wizard = new RebaseWizard(hgRoot);
			WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
			wizardDialog.open();
		} else {
			rebaseOnTip();
		}
	}

	private void rebaseOnTip() {
		IProgressService iProgressService = PlatformUI.getWorkbench().getProgressService();
		try {
			iProgressService.run(true, false, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					boolean useExternalMergeTool = MercurialEclipsePlugin.getDefault().getPreferenceStore()
						.getBoolean(MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE);

					boolean rebaseConflict = false;
					try {
						monitor.beginTask("Rebasing on tip", 2);
						// rebase on Tip revision
						HgRebaseClient.rebase(hgRoot, -1, -1, -1, false, false, false, false, false, useExternalMergeTool, null);

						monitor.worked(1);
						monitor.setTaskName("Updating");
						// if rebase succeeded try again updating to tip
						HgUpdateClient.update(hgRoot, null, cleanUpdateRequested);

						monitor.done();
					} catch (final HgException e) {
						rebaseConflict = HgRebaseClient.isRebaseConflict(e);
						if (!rebaseConflict) {
							MercurialEclipsePlugin.logError(e);
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									MessageDialog.openError(getShell(), "Rebase error", e.getMessage());
								}
							});
						}
					} finally {
						RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL);
						if (rebaseConflict) {
							// do not join to avoid any potential deadlocks. listener is enough
							job.addJobChangeListener(new JobChangeAdapter() {
								@Override
								public void done(IJobChangeEvent event) {
									showMergeView();
								}
							});
						}
						job.schedule();
					}
				}
			});
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * Checks that working copy is clear and (if needed) asks user to revert dirty files
	 * @return false if user cancelled operation or an error occured
	 */
	private boolean clearAndContinue() {
		try {
			if (HgStatusClient.isDirty(hgRoot)) {
				boolean clearAndContinue = cleanUpdateRequested
						|| MessageDialog.openConfirm(getShell(), "Uncommited changes",
						Messages.getString("RevertDialog.uncommitedChanges"));
				if (clearAndContinue) {
					HgRevertClient.performRevertAll(new NullProgressMonitor(), hgRoot);
					return true;
				}
				return false;
			}
			return true;	// no changes found
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return false;
		}
	}

	/**
	 * show Merge view, as it offers to abort a merge and revise the automatically merged files
	 */
	private void showMergeView() {
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					MergeView view = (MergeView) MercurialEclipsePlugin.getActivePage()
							.showView(MergeView.ID);
					view.refresh(hgRoot);
				} catch (PartInitException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
			}
		};
		Display.getDefault().asyncExec(runnable);
	}

	private void mergePressed() {
		if (!clearAndContinue()) {
			return;
		}

		try {
			MergeHandler.determineMergeHeadAndMerge(hgRoot,
					getParentShell(), new NullProgressMonitor(), false, true);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			MessageDialog.openError(getShell(), "Merging error", e.getMessage());
		}
	}

	@Override
	protected Image getImage() {
		return getShell().getDisplay().getSystemImage(SWT.ICON_INFORMATION);
	}

	public void setClean(boolean clean) {
		this.cleanUpdateRequested = clean;
	}

}
