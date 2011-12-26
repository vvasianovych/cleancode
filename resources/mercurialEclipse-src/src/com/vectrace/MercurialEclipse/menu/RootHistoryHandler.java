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

import org.eclipse.team.ui.TeamUI;

import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author Andrei
 */
public class RootHistoryHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) {
		TeamUI.getHistoryView().showHistoryFor(hgRoot);
	}

}
