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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgInitClient;
import com.vectrace.MercurialEclipse.model.HgPath;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;

public class InitOperation extends HgOperation {

	private final IProject project;
	private final File newHgRoot;

	public InitOperation(IRunnableContext ctx, IProject project, File newHgRoot) {
		super(ctx);
		this.newHgRoot = newHgRoot;
		this.project = project;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("InitOperation.creatingRepo"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			monitor.beginTask(Messages.getString("InitOperation.share"), 3); //$NON-NLS-1$

			if (!HgPath.isHgRoot(newHgRoot)) {
				monitor.subTask(Messages.getString("InitOperation.call")); //$NON-NLS-1$
				HgInitClient.init(newHgRoot);
				monitor.worked(1);
			}
			monitor.subTask(Messages.getString("InitOperation.mapping.1") + " " + project.getName() //$NON-NLS-1$
					+ Messages.getString("InitOperation.mapping.2")); //$NON-NLS-1$
			RepositoryProvider.map(project, MercurialTeamProvider.class.getName());
			monitor.worked(1);
			project.touch(monitor);
			monitor.subTask(Messages.getString("InitOperation.schedulingRefresh")); //$NON-NLS-1$
			new RefreshStatusJob(Messages.getString("InitOperation.refresh.1") + " " + project //$NON-NLS-1$
					+ Messages.getString("InitOperation.refresh.2"), project) //$NON-NLS-1$
					.schedule();
			monitor.worked(1);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

}