/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.model.HgRoot;

public abstract class RunnableHandler extends RootHandler {

	@Override
	public abstract void run(HgRoot hgRoot) throws CoreException;

}
