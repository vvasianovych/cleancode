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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.RebaseOperation;

public class AbortRebaseHandler extends RunnableHandler {

	@Override
	public void run(HgRoot hgRoot) throws CoreException {
		try {
			RebaseOperation.createAbort(MercurialEclipsePlugin.getActiveWindow(), hgRoot).run(
					new NullProgressMonitor());
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.rethrow(e);
		} catch (InterruptedException e) {
			MercurialEclipsePlugin.rethrow(e);
		}
	}
}
