/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Andrei Loskutov - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog.Options;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.CommitHandler;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.utils.StringUtils;

public class CommitSynchronizeOperation extends SynchronizeModelOperation {
	private final IResource[] resources;
	private final List<WorkingChangeSet> changesets;

	public CommitSynchronizeOperation(
			ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, IResource[] resources, List<WorkingChangeSet> changesets) {
		super(configuration, elements);
		this.resources = resources;
		this.changesets = changesets;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Loading Commit Window...", 1);
		new SafeUiJob("Committing selected resources...") {

			@Override
			protected IStatus runSafe(IProgressMonitor moni) {
				if (resources.length == 0 && !hasFiles(changesets)) {
					MessageDialog.openInformation(getShell(), "Mercurial Commit", //$NON-NLS-1$
							"Please select at least one file to commit!"); //$NON-NLS-1$
					return super.runSafe(moni);
				}
				if(resources.length == 0) {
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
						boolean done = commit(commithandler, files.toArray(new IResource[0]));
						if(done) {
							cs.getGroup().committed(cs);
						}
					}
				} else {
					final CommitHandler commithandler = new CommitHandler();
					commit(commithandler, resources);
				}
				return super.runSafe(moni);
			}

		}.schedule();
		monitor.done();
	}

	protected boolean commit(final CommitHandler commithandler, IResource[] resources) {
		try {
			commithandler.run(Arrays.asList(resources));
			return commithandler.getResult() == Window.OK;
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return false;
	}

	static boolean hasFiles(List<WorkingChangeSet> set) {
		for (WorkingChangeSet cs : set) {
			if(!cs.getFiles().isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
