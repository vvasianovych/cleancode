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
import com.vectrace.MercurialEclipse.commands.HgBisectClient.Status;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
final class BisectMarkGoodAction extends BisectAbstractAction {

	BisectMarkGoodAction(MercurialHistoryPage mercurialHistoryPage) {
		super(Messages.BisectMarkGoodAction_markSelectionAsGood, mercurialHistoryPage);
		this.setDescription(Messages.BisectMarkGoodAction_markSelectionDescription
				+ Messages.BisectMarkGoodAction_markSelectionDescription2);
	}

	@Override
	protected void updateHistory(MercurialRevision rev, HgRoot root) {
		rev.setBisectStatus(Status.GOOD);
		super.updateHistory(rev, root);
	}

	@Override
	public boolean isEnabled() {
		return isBisectStarted();
	}

	@Override
	String callBisect(HgRoot root, ChangeSet cs) throws HgException {
		setBisectStarted(true);
		return HgBisectClient.markGood(root, cs);
	}
}