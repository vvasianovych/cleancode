/*******************************************************************************
 * Copyright (c) 2005-2010 Andrei Loskutov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.CompareAction;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author Andrei
 */
public class OpenMergeEditorAction extends SynchronizeModelAction {

	public OpenMergeEditorAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/merge.gif"));
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration config,
			IDiffElement[] elements) {
		IStructuredSelection sel = getStructuredSelection();
		Object object = sel.getFirstElement();
		final IFile file = MercurialEclipsePlugin.getAdapter(object, IFile.class);
		return new SynchronizeModelOperation(config, elements){
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				CompareAction compareAction = new CompareAction(file);
				compareAction.setEnableMerge(true);
				compareAction.run(null);
			}
		};
	}

	@Override
	protected final boolean updateSelection(IStructuredSelection selection) {
		boolean updateSelection = super.updateSelection(selection);
		if(!updateSelection){
			if(selection.size() != 1){
				return false;
			}
			Object object = selection.getFirstElement();
			return isSupported(object);
		}
		return updateSelection;
	}

	private boolean isSupported(Object object) {
		IFile adapter = MercurialEclipsePlugin.getAdapter(object, IFile.class);
		if (adapter != null) {
			return !MercurialStatusCache.getInstance().isClean(adapter)
				&& MercurialStatusCache.getInstance().isMergeInProgress(adapter);
		}
		return false;
	}
}
