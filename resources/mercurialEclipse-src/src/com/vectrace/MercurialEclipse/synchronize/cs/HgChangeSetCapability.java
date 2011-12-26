/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class HgChangeSetCapability extends ChangeSetCapability {


	public HgChangeSetCapability() {
		super();
	}

	@Override
	public HgChangesetsCollector createSyncInfoSetChangeSetCollector(
			ISynchronizePageConfiguration configuration) {
		return new HgChangesetsCollector(configuration);
	}

	@Override
	public ActiveChangeSetManager getActiveChangeSetManager() {
		return null;
	}

	@Override
	public ActiveChangeSet createChangeSet(ISynchronizePageConfiguration configuration,
			IDiff[] diffs) {
		return null;
	}

	@Override
	public boolean supportsActiveChangeSets() {
		return false;
	}

	@Override
	public SynchronizePageActionGroup getActionGroup() {
		return super.getActionGroup();
	}

	@Override
	public boolean enableChangeSetsByDefault() {
		return true;
	}

	@Override
	public boolean supportsCheckedInChangeSets() {
		return true;
	}

}
