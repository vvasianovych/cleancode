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
package com.vectrace.MercurialEclipse.storage;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.HgRoot;

public class RootDataLoader extends DataLoader {

	private final HgRoot hgRoot;

	public RootDataLoader(HgRoot hgRoot) {
		this.hgRoot = hgRoot;
	}

	@Override
	public IResource getResource() {
		return null;
	}

	@Override
	public HgRoot getHgRoot() {
		return hgRoot;
	}

}