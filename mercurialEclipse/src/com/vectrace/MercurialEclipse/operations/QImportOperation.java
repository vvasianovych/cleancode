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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQImportClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.views.PatchQueueView;

/**
 * @author bastian
 *
 */
public class QImportOperation extends HgOperation {

	private final IPath patchFile;
	private final ChangeSet[] changesets;
	private final boolean existing;
	private final boolean force;
	private final IResource resource;

	/**
	 * @param context
	 */
	public QImportOperation(IRunnableContext context, IPath patchFile, ChangeSet[] changesets,
			boolean existing, boolean force, IResource resource) {
		super(context);
		this.patchFile = patchFile;
		this.changesets = changesets;
		this.existing = existing;
		this.force = force;
		this.resource = resource;

	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(getActionDescription(), 3);
		try {
			monitor.worked(1);
			monitor.subTask(Messages.getString("QImportOperation.call")); //$NON-NLS-1$
			HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
			if(hgRoot == null) {
				throw new InvocationTargetException(new IllegalStateException(
						"No hg root found for: " + resource));
			}
			HgQImportClient.qimport(hgRoot, force, existing, changesets, patchFile);
			monitor.worked(1);
			monitor.subTask(Messages.getString("QImportOperation.refreshingView")); //$NON-NLS-1$
			new SafeUiJob(Messages.getString("QImportOperation.refreshingView")) { //$NON-NLS-1$

				@Override
				protected IStatus runSafe(IProgressMonitor monitor1) {
					PatchQueueView.getView().populateTable();
					return super.runSafe(monitor1);
				}
			}.schedule();
			monitor.worked(1);
		} catch (HgException e) {
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		} finally {
			monitor.done();
		}
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("QImportOperation.importingPatch"); //$NON-NLS-1$
	}

}
