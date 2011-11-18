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
package com.vectrace.MercurialEclipse.team;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.operations.ResolveOperation;

public class ActionResolve implements IWorkbenchWindowActionDelegate {
	private IStructuredSelection selection;

	public void dispose() {
	}

	public void init(IWorkbenchWindow arg0) {
	}

	public void run(IAction action) {
		IWorkbenchPart part = MercurialEclipsePlugin.getActivePage().getActivePart();

		try {
			new ResolveOperation(part, selection.toArray(), true).run();
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.showError(e.getTargetException());
		} catch (InterruptedException e) {
			MercurialEclipsePlugin.showError(e);
		}
	}

	public void selectionChanged(IAction action, ISelection inSelection) {
		if (inSelection instanceof IStructuredSelection) {
			selection = (IStructuredSelection) inSelection;
		}
	}

}
