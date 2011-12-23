/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 *     Andrei Loskutov - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

/**
 * RemoveRootAction removes a repository
 */
public class RemoveRootAction extends SelectionListenerAction {
	private IStructuredSelection selection;

	public RemoveRootAction(Shell shell) {
		super("Remove");
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("rem_co.gif"));
	}

	/**
	 * Returns the selected remote files
	 */
	@SuppressWarnings("unchecked")
	protected IHgRepositoryLocation[] getSelectedRemoteRoots() {
		ArrayList<IHgRepositoryLocation> resources = new ArrayList<IHgRepositoryLocation>();
		if (selection != null && !selection.isEmpty()) {
			Iterator<IHgRepositoryLocation> elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = MercurialEclipsePlugin.getAdapter(elements.next(),
						IHgRepositoryLocation.class);
				if (next instanceof IHgRepositoryLocation) {
					resources.add((IHgRepositoryLocation) next);
				}
			}
		}
		return resources.toArray(new IHgRepositoryLocation[resources.size()]);
	}

	@Override
	public void run() {
		IHgRepositoryLocation[] roots = getSelectedRemoteRoots();
		if (roots.length == 0) {
			return;
		}
		boolean confirm = MessageDialog.openConfirm(null, "Mercurial Repositories",
				"Remove repository (all authentication data will be lost)?");
		if(!confirm){
			return;
		}
		for (IHgRepositoryLocation repo : roots) {
			MercurialEclipsePlugin.getRepoManager().disposeRepository(repo);
		}
	}

	/**
	 * updates the selection. this selection will be used during run returns
	 * true if action can be enabled
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection sel) {
		this.selection = sel;

		IHgRepositoryLocation[] roots = getSelectedRemoteRoots();
		return roots.length > 0;
	}

}
