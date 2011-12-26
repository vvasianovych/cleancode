/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.ChangeSet;

public class SwitchToSynchronizeAction extends ExportPatchSynchronizeAction {

	private boolean isParent;
	private final String parentText;
	private final String normalText;

	public SwitchToSynchronizeAction(String normalText, String parentText, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(normalText, configuration, selectionProvider);

		this.normalText = normalText;
		this.parentText = parentText;
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/switch.gif"));
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.actions.ExportPatchSynchronizeAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		ChangeSet cs = getChangeSet(selection);

		isParent = cs != null && cs.isCurrent();
		setText(isParent ? parentText : normalText);

		return super.updateSelection(selection);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.actions.ExportPatchSynchronizeAction#getSubsciberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration,
	 *      org.eclipse.compare.structuremergeviewer.IDiffElement[],
	 *      com.vectrace.MercurialEclipse.model.ChangeSet)
	 */
	@Override
	protected SynchronizeModelOperation getSubsciberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements, final ChangeSet cs) {
		final boolean isParentMode = this.isParent;
		return new SynchronizeModelOperation(configuration, elements) {
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				if (cs != null && cs.getHgRoot() != null) {
					UpdateHandler update = new UpdateHandler(false);
					HgRevision rev;

					update.setCleanEnabled(true);
					if (isParentMode) {
						rev = cs.getParentRevision(0, true);

						if (rev == null && cs.getRevision().getRevision() == 0) {
							return;
						}
					} else {
						rev = cs.getRevision();
					}
					if (rev == null) {
						MercurialEclipsePlugin.logError(new IllegalStateException("Missing revision"));
						return;
					}
					update.setRevision(rev);
					update.setShell(getShell());
					try {
						update.run(cs.getHgRoot());
					} catch (HgException e) {
						throw new InvocationTargetException(e);
					}
				}
			}
		};
	}
}
