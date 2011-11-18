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
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.handlers.HandlerUtil;

public class SearchHandler extends AbstractHandler {

	/**
	 * Mercurial search page id, as defined in plugin.xml
	 */
	public static final String MERCURIAL_ECLIPSE_SEARCH_PAGE = "MercurialEclipse.searchPage";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		NewSearchUI.openSearchDialog(HandlerUtil.getActiveWorkbenchWindow(event),
				MERCURIAL_ECLIPSE_SEARCH_PAGE);
		return null;
	}

	public static class SearchAction implements IWorkbenchWindowActionDelegate {

		private IWorkbenchWindow window;

		public void run(IAction action) {
			NewSearchUI.openSearchDialog(window, MERCURIAL_ECLIPSE_SEARCH_PAGE);
		}

		public void selectionChanged(IAction action, ISelection selection) {
			// noop
		}

		public void dispose() {
			window = null;
		}

		public void init(IWorkbenchWindow window1) {
			this.window = window1;
		}

	}

}
