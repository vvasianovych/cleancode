/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Console helper that allows contributing actions to the console view when
 * the Mercurial console is visible. Added to the console via an extension point
 * from org.eclipse.ui.console.
 */
public class HgConsolePageParticipant implements IConsolePageParticipant {

	private CloseConsoleAction consoleRemoveAction;

	public void init(IPageBookViewPage page, IConsole console) {
		consoleRemoveAction = new CloseConsoleAction(console);
		IActionBars bars = page.getSite().getActionBars();
		bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
	}

	public void dispose() {
		this.consoleRemoveAction = null;
	}

	public void activated() {
	}

	public void deactivated() {
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}
}
