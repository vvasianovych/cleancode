/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch				- Adaption to Mercurial
 *     Andrei Loskutov              - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Get action that appears in the synchronize view. It's main purpose is to
 * filter the selection and delegate its execution to the get operation.
 */
public abstract class AbstractResourceSynchronizeAction extends PathAwareAction {

	public AbstractResourceSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);

		ImageDescriptor descriptor = createImageDescriptor();

		if (descriptor != null) {
			setImageDescriptor(descriptor);
		}
	}

	protected ImageDescriptor createImageDescriptor() {
		return null;
	}

	@Override
	final protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		List<IResource> selectedResources = new ArrayList<IResource>();
		List<WorkingChangeSet> changesets = new ArrayList<WorkingChangeSet>();
		Object[] objects = getNormalizedSelection();
		for (Object object : objects) {
			if (object instanceof IResource) {
				selectedResources.add((IResource) object);
			} else if (object instanceof IAdaptable){
				IAdaptable adaptable = (IAdaptable) object;
				IResource resource = (IResource) adaptable.getAdapter(IResource.class);
				if(resource != null){
					selectedResources.add(resource);
				}
			} else if (object instanceof WorkingChangeSet){
				changesets.add((WorkingChangeSet) object);
			}
		}
		IResource[] resources = new IResource[selectedResources.size()];
		selectedResources.toArray(resources);
		return createOperation(configuration, elements, resources, changesets);
	}

	abstract protected SynchronizeModelOperation createOperation(ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, IResource[] resources, List<WorkingChangeSet> changesets);

	@Override
	protected boolean isSupported(Object object) {
		if (object instanceof ChangeSet) {
			return object instanceof WorkingChangeSet
					&& ((WorkingChangeSet) object).getFiles().size() > 0;
		}
		IResource resource = ResourceUtils.getResource(object);
		if (resource == null) {
			return false;
		}
		if (object instanceof FileFromChangeSet) {
			return ((FileFromChangeSet) object).getChangeset() instanceof WorkingChangeSet
					&& isResourceSupported(resource);
		}

		return isResourceSupported(resource);
	}

	/**
	 * Template method to determine if this action should be enabled for the given resource
	 *
	 * @param resource The resource to check
	 * @return True if this action should be enabled
	 */
	protected boolean isResourceSupported(IResource resource) {
		return !MercurialStatusCache.getInstance().isClean(resource);
	}
}
