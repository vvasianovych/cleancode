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
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog.Options;
import com.vectrace.MercurialEclipse.menu.CommitHandler;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.team.ActionRevert;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @author Andrei
 *
 */
public class RevertSynchronizeOperation extends SynchronizeModelOperation {
	private IResource[] resources;
	private final List<WorkingChangeSet> changesets;

	public RevertSynchronizeOperation(
			ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, IResource[] resources, List<WorkingChangeSet> changesets) {
		super(configuration, elements);
		this.resources = resources;
		this.changesets = changesets;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		new SafeUiJob("Reverting selected resources...") {

			@Override
			protected IStatus runSafe(IProgressMonitor moni) {
				if (resources.length == 0 && !CommitSynchronizeOperation.hasFiles(changesets)) {
					MessageDialog.openInformation(getShell(), "Mercurial Revert", //$NON-NLS-1$
							"Please select at least one file to revert!"); //$NON-NLS-1$
					return Status.OK_STATUS;
				}
				if(resources.length != 0) {
					revert(moni);
					return Status.OK_STATUS;
				}
				for (WorkingChangeSet cs : changesets) {
					if(cs.getFiles().isEmpty()) {
						continue;
					}
					CommitHandler commithandler = new CommitHandler();
					Options options = new Options();
					if(!StringUtils.isEmpty(cs.getComment())) {
						options.defaultCommitMessage = cs.getComment();
					} else {
						options.defaultCommitMessage = cs.getName();
					}
					commithandler.setOptions(options);
					List<IResource> files = new ArrayList<IResource>();
					files.addAll(cs.getFiles());
					resources = files.toArray(new IResource[0]);
					revert(moni);
				}
				return Status.OK_STATUS;
			}

		}.schedule();
		monitor.done();
	}

	protected void revert(IProgressMonitor moni) {
		final ActionRevert revert = new ActionRevert();
		revert.selectionChanged(null, new StructuredSelection(resources));
		revert.run(null);
	}

}
