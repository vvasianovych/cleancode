/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov 			- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.properties;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Show details on a selected mercurial model object.
 */
public class OpenPropertiesAction implements IObjectActionDelegate {

	private IWorkbenchPart targetPart;

	public OpenPropertiesAction() {
		super();
	}

	public OpenPropertiesAction(IWorkbenchPart targetPart) {
		super();
		this.targetPart = targetPart;
	}

	public final void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public final void selectionChanged(final IAction action, final ISelection newSelection) {
		// noop
	}

	public final void run(final IAction action) {
		try {
			if (targetPart == null) {
				MercurialEclipsePlugin.getActivePage().showView(IPageLayout.ID_PROP_SHEET);
			} else {
				targetPart.getSite().getPage().showView(IPageLayout.ID_PROP_SHEET);
			}
		} catch (PartInitException e) {
			MercurialEclipsePlugin.logError("Failed to show properties view", e);
		} finally {
			targetPart = null;
		}
	}

}
