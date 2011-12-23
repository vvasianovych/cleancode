/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ilya Ivanov	implementation
 *     Andrei Loskutov         - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This delegate is used for object contribution to context menus.
 * Object class must be FileFromChangeSet or be adaptable to it.
 */
public class OpenCurrentVersionDelegate implements IObjectActionDelegate {

	private FileFromChangeSet fileFromChangeSet;
	private IWorkbenchPart targetPart;

	public void run(IAction action) {
		ResourceUtils.openEditor(targetPart.getSite().getPage(), fileFromChangeSet.getFile());
	}

	public void selectionChanged(IAction action, ISelection newSelection) {
		action.setEnabled(false);
		if (newSelection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) newSelection;
			if (sel.getFirstElement() instanceof FileFromChangeSet) {
				this.fileFromChangeSet = (FileFromChangeSet) sel.getFirstElement();
				IFile file = fileFromChangeSet.getFile();
				if (file.exists()) {
					action.setEnabled(true);
				}
			}
		} else {
			this.fileFromChangeSet = null;
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
}
