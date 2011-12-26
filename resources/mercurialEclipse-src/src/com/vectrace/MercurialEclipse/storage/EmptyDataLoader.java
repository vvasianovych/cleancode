/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;

/**
 * Simple loader which does nothing
 *
 * @author Andrei
 */
public final class EmptyDataLoader extends DataLoader {
	@Override
	public HgRoot getHgRoot() {
		return null;
	}

	@Override
	public IResource getResource() {
		return null;
	}

	@Override
	public Branch[] getBranches() throws HgException {
		return new Branch[0];
	}

	@Override
	public ChangeSet[] getHeads() throws HgException {
		return new ChangeSet[0];
	}

	@Override
	public int[] getParents() throws HgException {
		return new int[0];
	}

	@Override
	public Tag[] getTags() throws HgException {
		return new Tag[0];
	}
}