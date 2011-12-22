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
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class ResolveOperation extends HgOperation {

	private final boolean checkStatus;
	private final Object[] resources;

	/**
	 * @param part
	 *            The workbench part
	 * @param resources
	 *            The resources to resolve
	 * @param checkStatus
	 *            Whether the status of the resources has already been checked
	 */
	public ResolveOperation(IWorkbenchPart part, Object[] resources, boolean checkStatus) {
		super(part);

		this.resources = resources;
		this.checkStatus = checkStatus;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("ResolveOperation.resolving"); //$NON-NLS-1$;
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			Object[] filtered = filter(resources);

			monitor.beginTask(getActionDescription(), filtered.length);

			if (filtered.length == 0) {
				throw new HgException("Please select conflicted files to resolve");
			}

			for (Object resource : filtered) {
				if (resource instanceof IFile) {
					HgResolveClient.markResolved((IFile) resource);
				}
			}
		} catch (HgException e) {
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		} finally {
			monitor.worked(1);
			monitor.done();
		}
	}

	private Object[] filter(Object[] objs) {
		List<Object> l = new ArrayList<Object>();
		MercurialStatusCache cache = MercurialStatusCache.getInstance();

		// Do not include projects or folders. One of the reasons is we'd have to ask for
		// confirmation.

		for (Object cur : objs) {
			if (cur instanceof IFile) {
				if (!checkStatus || cache.isConflict((IFile) cur)) {
					l.add(cur);
				}
			}
		}

		return l.toArray();
	}

}
