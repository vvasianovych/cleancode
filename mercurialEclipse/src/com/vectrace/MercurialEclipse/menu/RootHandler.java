/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Andrei
 */
public abstract class RootHandler extends AbstractHandler {

	private HgRoot selection;
	private Shell shell;

	protected Shell getShell() {
		return shell != null? shell : MercurialEclipsePlugin.getActiveShell();
	}

	protected HgRoot getSelectedRoot() {
		return selection;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selectionObject = HandlerUtil.getCurrentSelection(event);
		try {
			if (selectionObject != null && selectionObject instanceof IStructuredSelection) {
				Object listEntry = ((IStructuredSelection)selectionObject).getFirstElement();
				if (listEntry instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) listEntry;
					selection = MercurialEclipsePlugin.getAdapter(adaptable, HgRoot.class);
				}
					if(selection == null){
					IResource resource = ResourceUtils.getResource(listEntry);
						if(resource != null){
							selection = MercurialTeamProvider.getHgRoot(resource);
						}
					}
				}
			if (selection == null) {
				IFile file = ResourceUtils.getActiveResourceFromEditor();
				if(file != null){
					selection = MercurialTeamProvider.getHgRoot(file);
				}
			}

			if(selection == null){
				MessageDialog.openError(getShell(), "Error", "Hg root not known!");
				return null;
			}
			run(getSelectedRoot());
		} catch (CoreException e) {
			MessageDialog
					.openError(
							getShell(),
							Messages.getString("SingleResourceHandler.hgSays"), e.getMessage() + Messages.getString("SingleResourceHandler.seeErrorLog")); //$NON-NLS-1$ //$NON-NLS-2$
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @param shell the shell to set, may be null
	 */
	public void setShell(Shell shell) {
		this.shell = shell;
	}

	/**
	 * @param hgRoot never null
	 * @throws HgException
	 */
	protected abstract void run(HgRoot hgRoot) throws CoreException;
}