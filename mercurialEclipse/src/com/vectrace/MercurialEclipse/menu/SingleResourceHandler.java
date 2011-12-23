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
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public abstract class SingleResourceHandler extends AbstractHandler {

	private IResource selection;
	private Shell shell;

	protected Shell getShell() {
		return shell != null? shell : MercurialEclipsePlugin.getActiveShell();
	}

	protected IResource getSelectedResource() {
		return selection;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		selection = null;
		ISelection selectionObject = HandlerUtil.getCurrentSelection(event);
		try {
			if (selectionObject instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selectionObject;
				selection = ResourceUtils.getResource(ssel.getFirstElement());
			}
			if (selection == null) {
				selection = ResourceUtils.getActiveResourceFromEditor();
			}

			run(getSelectedResource());
		} catch (Exception e) {
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

	protected abstract void run(IResource resource) throws Exception;
}