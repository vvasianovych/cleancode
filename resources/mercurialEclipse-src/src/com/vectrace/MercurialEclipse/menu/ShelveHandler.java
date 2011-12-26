/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.ShelveOperation;

/**
 * @author bastian
 */
public class ShelveHandler extends RootHandler {

	@Override
	protected void run(final HgRoot hgRoot) {
		new SafeWorkspaceJob(Messages.getString("ShelveHandler.Shelving")) { //$NON-NLS-1$

			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				try {
					ShelveOperation op = new ShelveOperation((IWorkbenchPart) null,	hgRoot);
					op.run(monitor);
					return super.runSafe(monitor);
				} catch (InvocationTargetException e) {
					return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
							0, e.getLocalizedMessage(), e);
				} catch (InterruptedException e) {
					return new Status(IStatus.INFO, MercurialEclipsePlugin.ID,
							0, e.getLocalizedMessage(), e);
				}
			}
		}.schedule();

	}

}
