/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		adam.berkes	- implementation
 * 		Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class DeleteAction extends SynchronizeModelAction {

	public static final String HG_DELETE_GROUP = "hg.delete";
	private final ISynchronizePageConfiguration configuration;

	public DeleteAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_ETOOL_DELETE));
		this.configuration = configuration;
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration config,
			IDiffElement[] elements) {
		List<IResource> selectedResources = new ArrayList<IResource>();
		IStructuredSelection sel = getStructuredSelection();
		if(sel.size() == 1 && sel.getFirstElement() instanceof WorkingChangeSet) {
			final WorkingChangeSet changeSet = (WorkingChangeSet) sel.getFirstElement();
			return new SynchronizeModelOperation(config, elements) {
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					changeSet.getGroup().delete(changeSet);
				}
			};
		}
		Object[] objects = sel.toArray();
		for (Object object : objects) {
			if (object instanceof WorkingChangeSet){
				selectedResources.addAll(((WorkingChangeSet) object).getFiles());
			} else if(!(object instanceof ChangeSet)){
				IResource resource = ResourceUtils.getResource(object);
				if(resource != null) {
					selectedResources.add(resource);
				}
			}
		}
		IResource[] resources = new IResource[selectedResources.size()];
		selectedResources.toArray(resources);
		return new DeleteOperation(configuration, elements, resources);
	}

	@Override
	protected final boolean updateSelection(IStructuredSelection selection) {
		boolean updateSelection = super.updateSelection(selection);
		if(!updateSelection){
			if(selection.size() == 1 && selection.getFirstElement() instanceof WorkingChangeSet) {
				WorkingChangeSet changeSet = (WorkingChangeSet) selection.getFirstElement();
				if(changeSet.isDefault()) {
					return false;
				}
				return true;
			}
			Object[] array = selection.toArray();
			for (Object object : array) {
				if(!isSupported(object)){
					return false;
				}
			}
			return true;
		}
		return updateSelection;
	}

	private boolean isSupported(Object object) {
		IResource adapter = MercurialEclipsePlugin.getAdapter(object, IResource.class);
		if (adapter != null) {
			return true;
		}
		return false;
	}
}
