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

import org.eclipse.ui.console.IConsoleFactory;

/**
 * Console factory is used to show the console from the Console view
 * "Open Console" drop-down action. This factory is registered via the
 * org.eclipse.ui.console.consoleFactory extension point.
 *
 * @since 3.1
 */
/*
 * Eclipse creates an instance of this class before calling openConsole so we must make it
 * stateless even though we want to have at most a single console.
 */
public class HgConsoleFactory implements IConsoleFactory {

	public HgConsoleFactory() {
	}

	public void openConsole() {
		HgConsoleHolder.getInstance().showConsole(true);
	}
}
