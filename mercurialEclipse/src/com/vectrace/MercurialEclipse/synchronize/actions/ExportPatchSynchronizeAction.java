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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.wizards.ExportPatchWizard;

public class ExportPatchSynchronizeAction extends SynchronizeModelAction {

	public ExportPatchSynchronizeAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);

		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("export.gif"));
	}

	// operations

	protected ChangeSet getChangeSet(IStructuredSelection selection) {
		Object el;
		if (selection.size() == 1 && (el = selection.getFirstElement()) instanceof ChangeSet
				&& ((ChangeSet) el).getDirection() == Direction.OUTGOING) {
			return (ChangeSet) el;
		}
		return null;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.actions.PathAwareAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection); // Needed?
		return getChangeSet(selection) != null;
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration,
	 *      org.eclipse.compare.structuremergeviewer.IDiffElement[])
	 */
	@Override
	protected final SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return getSubsciberOperation(configuration, elements,
				getChangeSet(getStructuredSelection()));
	}

	protected SynchronizeModelOperation getSubsciberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements, final ChangeSet cs) {

		return new SynchronizeModelOperation(configuration, elements) {
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				if (cs != null) {
					getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							ExportPatchWizard wizard = new ExportPatchWizard(cs);
							WizardDialog dialog = new WizardDialog(getShell(), wizard);
							dialog.setBlockOnOpen(true);
							dialog.open();
						}
					});
				}
			}
		};
	}
}
