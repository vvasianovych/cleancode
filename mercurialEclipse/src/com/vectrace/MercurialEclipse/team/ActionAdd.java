/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.AddHandler;

public class ActionAdd implements IWorkbenchWindowActionDelegate {
	private IStructuredSelection selection;

	public ActionAdd() {
		super();
	}

	public void dispose() {

	}

	public void init(IWorkbenchWindow w) {
	}

	private List<IResource> getSelectedResources() {
		List<IResource> l = new ArrayList<IResource>();
		for (Object o : selection.toList()) {
			l.add((IResource) o);
		}
		return l;
	}

	public void run(IAction action) {
		try {
			new AddHandler().run(getSelectedResources());
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	public void selectionChanged(IAction action, ISelection inSelection) {
		if (inSelection != null
				&& inSelection instanceof IStructuredSelection) {
			selection = (IStructuredSelection) inSelection;
		}
	}

}
