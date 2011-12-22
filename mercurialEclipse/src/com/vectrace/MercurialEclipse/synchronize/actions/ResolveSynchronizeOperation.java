/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.operations.ResolveOperation;

/**
 * @see ResolveSynchronizeAction
 */
public class ResolveSynchronizeOperation extends SynchronizeModelOperation {

	private final IResource[] resources;

	public ResolveSynchronizeOperation(ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Resolving...", 1);
		new SafeUiJob("Resolving selected resources...") {

			@Override
			protected IStatus runSafe(IProgressMonitor moni) {
				try {
					new ResolveOperation(MercurialEclipsePlugin.getActivePage().getActivePart(),
							resources, false).run(moni);
				} catch (InvocationTargetException e1) {
					return getStatus(e1.getTargetException());
				} catch (InterruptedException e1) {
					return getStatus(e1);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
		monitor.done();
	}

	protected IStatus getStatus(Throwable t) {
		if (t instanceof TeamException) {
			return ((TeamException) t).getStatus();
		}

		return new HgException("Error resolving files", t).getStatus();
	}
}
