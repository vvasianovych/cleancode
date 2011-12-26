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
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;

import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author zingo
 *
 */
public class MercurialHistoryProvider extends FileHistoryProvider {

	public MercurialHistoryProvider() {
		super();
	}

	public IFileHistory getFileHistoryFor(IResource resource, int flags, IProgressMonitor monitor) {
		RepositoryProvider provider = RepositoryProvider.getProvider(((IFile) resource)
				.getProject());
		if (provider instanceof MercurialTeamProvider) {
			return new MercurialHistory(resource);
		}
		return null;
	}

	public IFileHistory getFileHistoryFor(IFileStore store, int flags, IProgressMonitor monitor) {
		return null;
	}

	public IFileRevision getWorkspaceFileRevision(IResource resource) {
		return null;
	}
}
