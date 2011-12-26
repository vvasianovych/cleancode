/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

/**
 * @author bastian
 *
 */
public class MercurialSyncInfo extends SyncInfo {

	protected final int description;

	public MercurialSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote, IResourceVariantComparator comparator, int description) {
		super(local, base, remote, comparator);
		this.description = description;
	}

	@Override
	protected int calculateKind() throws TeamException {
		if(description >= 0){
			return description;
		}
		return super.calculateKind();
	}

	// here to avoid FindBugs warnings
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
