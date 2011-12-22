/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.runtime.IAdaptable;

import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class FlaggedAdaptable implements IAdaptable {

	private final IAdaptable adaptable;
	private final char flag;


	public FlaggedAdaptable(IAdaptable adaptable, char flag) {
		this.adaptable = adaptable;
		this.flag = flag;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if(adaptable == null){
			return null;
		}
		return adaptable.getAdapter(adapter);
	}

	public char getFlag() {
		return this.flag;
	}

	public String getStatus() {
		return flag == MercurialStatusCache.CHAR_UNRESOLVED
				? Messages.getString("FlaggedAdaptable.unresolvedStatus")
				: Messages.getString("FlaggedAdaptable.resolvedStatus");
	}
}
