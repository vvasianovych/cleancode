/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jérôme Nègre              - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - extracted class since I need it for sync
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;

public abstract class DataLoader {

	public abstract HgRoot getHgRoot();
	public abstract IResource getResource();

	public Tag[] getTags() throws HgException {
		return HgTagClient.getTags(getHgRoot());
	}

	public Branch[] getBranches() throws HgException {
		return HgBranchClient.getBranches(getHgRoot());
	}

	public ChangeSet[] getHeads() throws HgException {
		return HgLogClient.getHeads(getHgRoot());
	}

	public int[] getParents() throws HgException {
		return HgParentClient.getParents(getHgRoot());
	}

}