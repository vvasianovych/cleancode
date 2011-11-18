/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;

/**
 * @author Andrei
 */
public class RevertSynchronizeAction  extends AbstractResourceSynchronizeAction {

	public RevertSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
	}

	@Override
	protected ImageDescriptor createImageDescriptor() {
		return MercurialEclipsePlugin.getImageDescriptor("actions/revert.gif");
	}

	@Override
	protected SynchronizeModelOperation createOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements,
			IResource[] resources, List<WorkingChangeSet> changesets) {
		return new RevertSynchronizeOperation(configuration, elements, resources, changesets);
	}
}
