/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

public class SafeUiJob extends UIJob {

	/**
	 * @param name
	 */
	public SafeUiJob(String name) {
		super(name);
	}

	/**
	 * @param jobDisplay
	 * @param name
	 */
	public SafeUiJob(Display jobDisplay, String name) {
		super(jobDisplay, name);
	}

	@Override
	public final IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			return runSafe(monitor);
		} catch (RuntimeException error) {
			MercurialEclipsePlugin.logError(error);
			return Status.CANCEL_STATUS;
		}
	}

	/**
	 * @param monitor
	 * @return
	 */
	protected IStatus runSafe(IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

}
