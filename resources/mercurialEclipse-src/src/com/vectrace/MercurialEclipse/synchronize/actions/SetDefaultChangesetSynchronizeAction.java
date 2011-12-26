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
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.navigator.CommonViewer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider;

/**
 * Get action that appears in the synchronize view. It's main purpose is to
 * filter the selection and delegate its execution to the get operation.
 */
public class SetDefaultChangesetSynchronizeAction extends SynchronizeModelAction {

	public static final String ID = "hg.setDefaultChangeset";

	public SetDefaultChangesetSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		setId(ID);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("elcl16/uncommitted_cs.gif", "ovr/pinned_ovr.gif",
				IDecoration.TOP_RIGHT));
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			final ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IStructuredSelection sel = getStructuredSelection();
		// it's guaranteed that we have exact one element
		final Object object = sel.getFirstElement();
		if(object instanceof WorkingChangeSet){
			return new SynchronizeModelOperation(configuration, elements) {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					Viewer viewer = configuration.getPage().getViewer();
					if(!(viewer instanceof ContentViewer)){
						return;
					}
					CommonViewer commonViewer = (CommonViewer) viewer;
					final HgChangeSetContentProvider csProvider = OpenAction.getProvider(commonViewer.getNavigatorContentService());
					csProvider.getUncommittedCsManager().makeDefault((WorkingChangeSet) object);
				}

			};
		}
		return null;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		boolean updateSelection = super.updateSelection(selection);
		if(!updateSelection){
			Object[] array = selection.toArray();
			if(selection.size() != 1){
				return false;
			}
			return isSupported(array[0]);
		}
		return updateSelection;
	}

	private boolean isSupported(Object object) {
		return object instanceof WorkingChangeSet;
	}

}
