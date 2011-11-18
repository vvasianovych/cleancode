/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.history.IHistoryPageSource;

import com.vectrace.MercurialEclipse.history.MercurialHistoryPageSource;
import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class AdapterFactory implements IAdapterFactory {

	@SuppressWarnings("unchecked")
	public MercurialHistoryPageSource getAdapter(Object adaptableObject, Class adapterType) {
		if((adaptableObject instanceof MercurialHistoryProvider) && adapterType == IHistoryPageSource.class) {
			return new MercurialHistoryPageSource((MercurialHistoryProvider) adaptableObject);
		}
		if((adaptableObject instanceof HgRoot) && adapterType == IHistoryPageSource.class) {
			return new MercurialHistoryPageSource(null);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Class<IHistoryPageSource>[] getAdapterList() {
		return new Class[] { IHistoryPageSource.class };
	}
}
