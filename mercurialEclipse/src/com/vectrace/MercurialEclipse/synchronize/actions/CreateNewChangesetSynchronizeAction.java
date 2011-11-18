/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov.
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
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;

/**
 * Creates new empty uncommitted changeset with default name
 */
public class CreateNewChangesetSynchronizeAction extends SynchronizeModelAction {

	public static final String ID = "hg.createNewChangeset";

	public CreateNewChangesetSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		setId(ID);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("elcl16/uncommitted_cs.gif", "ovr/add_ovr.gif",
				IDecoration.TOP_RIGHT));
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			final ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IStructuredSelection sel = getStructuredSelection();
		// it's guaranteed that we have exact one element
		final Object object = sel.getFirstElement();
		if(object instanceof UncommittedChangesetGroup){
			return new SynchronizeModelOperation(configuration, elements) {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					Viewer viewer = configuration.getPage().getViewer();
					if(!(viewer instanceof ContentViewer)){
						return;
					}
					CommonViewer commonViewer = (CommonViewer) viewer;
					final HgChangeSetContentProvider csProvider = OpenAction.getProvider(commonViewer.getNavigatorContentService());
					csProvider.getUncommittedCsManager().getUncommittedGroup().create(new IFile[0]);
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

	private static boolean isSupported(Object object) {
		return object instanceof UncommittedChangesetGroup;
	}

}
