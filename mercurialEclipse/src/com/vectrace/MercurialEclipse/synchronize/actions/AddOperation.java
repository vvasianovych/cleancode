/*******************************************************************************
 * Copyright (c) 2005-2010 Andrei Loskutov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.AddHandler;

/**
 * @author Andrei
 */
public class AddOperation extends SynchronizeModelOperation {
	private final IResource[] resources;

	protected AddOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if(resources.length == 0){
			monitor.done();
			return;
		}

		new SafeUiJob("Adding selected resources...") {
			@Override
			protected IStatus runSafe(IProgressMonitor moni) {
				AddHandler handler = new AddHandler();
				try {
					handler.run(Arrays.asList(resources));
				} catch (HgException e) {
					MercurialEclipsePlugin.showError(e);
				}
				moni.done();
				return Status.OK_STATUS;
			}
		}.schedule();
		monitor.done();
	}

}
