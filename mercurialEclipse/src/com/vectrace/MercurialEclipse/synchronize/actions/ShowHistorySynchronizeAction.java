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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Get action that appears in the synchronize view. It's main purpose is to
 * filter the selection and delegate its execution to the get operation.
 */
public class ShowHistorySynchronizeAction extends SynchronizeModelAction {

	public static final String ID = "hg.history";

	public ShowHistorySynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		setId(ID);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("history.gif"));
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IStructuredSelection sel = getStructuredSelection();
		// it's guaranteed that we have exact one element
		Object object = sel.getFirstElement();
		if(object instanceof ChangeSet){
			ChangeSet changeSet = (ChangeSet) object;
			if(changeSet.getHgRoot() != null) {
				object = changeSet.getHgRoot();
			} else if (object instanceof WorkingChangeSet) {
				IProject[] projects = ((MercurialSynchronizeParticipant)configuration.getParticipant()).getContext().getScope().getProjects();
				Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(Arrays.asList(projects));
				Set<HgRoot> roots = byRoot.keySet();
				if (roots.size() == 1) {
					object = roots.iterator().next();
				}
			} else {
				IResource resource = ResourceUtils.getResource(object);
				if(resource != null){
					object = MercurialTeamProvider.getHgRoot(resource);
				}
			}
		} else {
			object = ResourceUtils.getResource(object);
		}
		return new ShowHistorySynchronizeOperation(configuration, elements,	object);
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
		IResource resource = ResourceUtils.getResource(object);
		if(resource != null){
			return true;
		}
		return object instanceof ChangeSet && isMatching(((ChangeSet) object)
						.getDirection());
	}

	private boolean isMatching(Direction d){
		return d == Direction.OUTGOING;
	}
}
