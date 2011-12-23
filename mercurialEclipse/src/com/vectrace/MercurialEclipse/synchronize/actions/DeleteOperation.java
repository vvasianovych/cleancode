/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * adam.berkes	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.actions.DeleteResourceAction;

import com.vectrace.MercurialEclipse.SafeUiJob;

/**
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class DeleteOperation extends SynchronizeModelOperation {
	private final IResource[] resources;

	protected DeleteOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, IResource[] resources) {
		super(configuration, elements);
		this.resources = resources;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if(resources.length == 0){
			monitor.done();
			return;
		}

		new SafeUiJob("Deleting selected resources...") {
			@Override
			protected IStatus runSafe(IProgressMonitor moni) {
				DeleteResourceAction resourceAction = new DeleteResourceAction(getShell()) {
					@Override
					protected List getSelectedResources() {
						return Arrays.asList(resources);
					}

					@Override
					protected boolean updateSelection(IStructuredSelection selection) {
						return true;
					}
				};
				resourceAction.selectionChanged(new StructuredSelection(resources));
				resourceAction.run();
				moni.done();
				return super.runSafe(moni);
			}
		}.schedule();
		monitor.done();
	}

}
