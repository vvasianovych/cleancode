/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov         - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class ActionShowRootHistory implements IWorkbenchWindowActionDelegate {

	private IStructuredSelection selection;

	public ActionShowRootHistory() {
		super();
	}

	public void dispose() {
		// noop
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		final IResource resource = (IResource) selection.getFirstElement();
		final HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
		if (hgRoot == null) {
			MercurialEclipsePlugin.showError(new IllegalStateException("There is no hg root for: "
					+ resource));
			return;
		}
		Runnable r = new Runnable() {
			public void run() {
				TeamUI.getHistoryView().showHistoryFor(hgRoot);
			}
		};
		Display.getDefault().asyncExec(r);
	}

	public void selectionChanged(IAction action, ISelection inSelection) {
		if (inSelection != null && inSelection instanceof IStructuredSelection) {
			selection = (IStructuredSelection) inSelection;
		}
	}

}
