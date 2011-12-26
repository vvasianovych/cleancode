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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * @author StefanC
 *
 */
public class SafeWorkspaceJob extends WorkspaceJob {

/**
   * @param name
   */
public SafeWorkspaceJob(String name) {
	super(name);
}

/*
   * (non-Javadoc)
   *
   * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
   */
@Override
public IStatus runInWorkspace(IProgressMonitor monitor)
	  throws CoreException {
	try {
	  return runSafe(monitor);
	} catch (RuntimeException ex) {
	  handleException(ex);
	  return Status.CANCEL_STATUS;
	}
}

/**
   * @return
   */
protected IStatus runSafe(IProgressMonitor monitor) {
	return Status.OK_STATUS;
}

protected void handleException(Throwable ex) {
	MercurialEclipsePlugin.logError(ex);

}


}
