/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ilya Ivanov (Intland)	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRollbackClient;
import com.vectrace.MercurialEclipse.menu.Messages;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author Ilya Ivanov (Intland)
 */
public class RollbackSynchronizeAction extends SynchronizeModelAction {

	private final HgRoot hgRoot;
//	private final ChangeSet changeSet;

	protected RollbackSynchronizeAction(String text, ISynchronizePageConfiguration configuration,
			HgRoot hgRoot, ChangeSet changeSet) {

		super(text, configuration);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/revert.gif"));

		this.hgRoot = hgRoot;
//		this.changeSet = changeSet;
	}

	private class RollbackSynchronizeOperation extends SynchronizeModelOperation {

		protected RollbackSynchronizeOperation(ISynchronizePageConfiguration configuration,
				IDiffElement[] elements) {
			super(configuration, elements);
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {

			try {
				final String result = HgRollbackClient.rollback(hgRoot);
				getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						MessageDialog.openInformation(getShell(),
								Messages.getString("RollbackHandler.output"), result); //$NON-NLS-1$
					}
				});
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {

		return new RollbackSynchronizeOperation(configuration, elements);
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return true;	// should be always available
	}
}
