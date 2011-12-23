/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * An empty (null) revision to be used for 0 (first/non existent) revisions in compare editor
 * @author Andrei
 */
public class NullRevision extends MercurialRevisionStorage {

	/**
	 * @param res
	 */
	public NullRevision(IFile res, ChangeSet cs) {
		super(res, 0, null, null);
		content = new ContentHolder((byte[]) null);
		changeSet = cs;
	}

	@Override
	public String getName() {
		return super.getName() + ": no content";
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

}
