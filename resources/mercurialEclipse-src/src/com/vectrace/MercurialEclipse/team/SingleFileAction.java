/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public abstract class SingleFileAction implements IActionDelegate {

	protected IFile selection;

	public SingleFileAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			//the xml enables this action only for a selection of a single file
			this.selection = (IFile) ((IStructuredSelection) sel).getFirstElement();
		}
	}

	protected Shell getShell() {
		return MercurialEclipsePlugin.getActiveShell();
	}

	protected IFile getSelectedFile() {
		return selection;
	}

	public void run(IAction action) {
		try {
			run(getSelectedFile());
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			MessageDialog.openError(getShell(), Messages.getString("SingleFileAction.hgSays"),
					e.getMessage() + Messages.getString("SingleFileAction.seeErrorLog"));
		}
	}

	protected abstract void run(IFile file) throws CoreException;
}