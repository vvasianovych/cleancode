/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * Handles resolve from the synchronize view
 */
public class ResolveSynchronizeAction extends AbstractResourceSynchronizeAction {

	public ResolveSynchronizeAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
	}

	@Override
	protected SynchronizeModelOperation createOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements,
			IResource[] resources, List<WorkingChangeSet> changesets) {

		return new ResolveSynchronizeOperation(configuration, elements, resources);
	}

	@Override
	protected boolean isResourceSupported(IResource resource) {
		return super.isResourceSupported(resource) && resource instanceof IFile
				&& MercurialStatusCache.getInstance().isConflict(resource);
	}
}
