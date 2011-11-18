/*******************************************************************************
 * Copyright (c) 2010 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch - Implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
final class BisectResetAction extends BisectAbstractAction {

	BisectResetAction(MercurialHistoryPage mercurialHistoryPage) {
		super(Messages.BisectResetAction_name, mercurialHistoryPage);
		this.setDescription(Messages.BisectResetAction_description
				+ Messages.BisectResetAction_description2);
	}

	@Override
	public boolean isEnabled() {
		return isBisectStarted();
	}

	@Override
	String callBisect(HgRoot root, ChangeSet cs) throws HgException {
		setBisectStarted(false);
		return HgBisectClient.reset(root);
	}

	@Override
	protected boolean checkDirty(HgRoot root) throws HgException {
		return false;
	}
}